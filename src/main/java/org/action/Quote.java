package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.*;
import org.database.DtoWithDistance;
import org.database.Persistable;
import org.database.dto.QuoteDto;
import org.database.dto.QuoteTagDto;
import org.database.dto.TagDto;
import org.exception.CustomException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.NativeQuery;
import org.parsing.ChatCommand;
import org.parsing.ChatConfirmation;
import org.parsing.Modifier;
import org.utility.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class Quote extends ActionHandler {
    static {
        ActionHandler.ACTION_MODIFIERS.put(
                Quote.ActionModifier.TYPE,
                new Modifier<>(Quote.TypeArgument.class, Quote.TypeArgument.GET_QUOTE, false, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                Quote.ActionModifier.TAG,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                Quote.ActionModifier.ORDER,
                new Modifier<>(Quote.OrderArgument.class, Quote.OrderArgument.NEWEST, false, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                Quote.ActionModifier.COUNT,
                new Modifier<>(Quote.CountArgument.class, 5L, false, false, true, false, 1L, Long.MAX_VALUE)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                Quote.ActionModifier.VALUE,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
    }

    private static long getCountModifierArgument(ChatCommand chatCommand, ProcessingContext processingContext) {
        TypedValue chatCount = chatCommand.getFirstArgument(Quote.ActionModifier.COUNT, false, true, processingContext);

        long resultsCount;
        if (chatCount.getType() == TypedValue.Type.WHOLE_NUMBER) {
            resultsCount = Long.parseLong(chatCount.getUsedValue());
        } else {
            ProcessingContext dummy = new ProcessingContext();
            assert chatCommand.getFirstArgumentAsEnum(Quote.ActionModifier.COUNT, Quote.CountArgument.class, true, dummy) == Quote.CountArgument.ALL;
            assert chatCount.getType() == TypedValue.Type.ENUMERATOR;

            resultsCount = Long.MAX_VALUE;
        }

        return resultsCount;
    }

    private static String processGetQuoteResult(List<QuoteDto> quotes, ChatCommand chatCommand, Session session) {
        boolean verboseSwitchPresent = chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.VERBOSE);
        if (verboseSwitchPresent) {
            quotes.forEach(quote -> quote.loadTagDtos(session));
        }

        StringBuilder stringBuilder = new StringBuilder();
        quotes.forEach(quote -> {
            stringBuilder.append(quote.getQuote());
            if (verboseSwitchPresent) {
                stringBuilder.append('\n').append(Helper.stringifyCollection(quote.getTagDtos(), TagDto::getTag, true));
            }
            stringBuilder.append("\n\n");
        });
        stringBuilder.setLength(stringBuilder.length() - 2);

        return stringBuilder.toString();
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        try {
            Quote.TypeArgument typeArgument = chatCommand.getFirstArgumentAsEnum(Quote.ActionModifier.TYPE, Quote.TypeArgument.class, true, processingContext);

            switch (typeArgument) {
                case Quote.TypeArgument.GET_QUOTE -> this.handleGetQuote(event, chatCommand, processingContext);
                case Quote.TypeArgument.GET_TAG -> this.handleGetTag(event, chatCommand, processingContext);
                case Quote.TypeArgument.NEW_QUOTE -> this.handleNewQuote(event, chatCommand, processingContext);
                case Quote.TypeArgument.NEW_TAG -> this.handleNewTag(event, chatCommand, processingContext);
            }
        } catch (CustomException exception) {
            processingContext.addMessages(exception.getMessage(), ProcessingContext.MessageType.ERROR);
        }
    }

    @Override
    protected Class<? extends Enum<?>> getModifierEnumClass() {
        return Quote.ActionModifier.class;
    }

    private void handleGetQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        Quote.OrderArgument chatOrder = chatCommand.getFirstArgumentAsEnum(Quote.ActionModifier.ORDER, Quote.OrderArgument.class, true, processingContext);
        ProcessingContext dummy = new ProcessingContext();
        Set<String> chatTags = chatCommand.getArguments(Quote.ActionModifier.TAG, true, true, dummy).stream()
                .map(argument -> argument.getTrimmedUsedValue(processingContext))
                .collect(Collectors.toSet());
        long resultsCount = Quote.getCountModifierArgument(chatCommand, processingContext);

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            if (!chatTags.isEmpty()) {
                List<String> nonExistentTags = TagDto.filterOutExistingTags(chatTags, event.getGuild().getId(), session);
                Check.isEmpty(nonExistentTags, true, "Non existent tags");
            }

            String sqlOrder = "";
            switch (chatOrder) {
                case Quote.OrderArgument.RANDOM -> sqlOrder = "rand()";
                case Quote.OrderArgument.NEWEST -> sqlOrder = QuoteDto.DATE_MODIFIED_COLUMN_NAME + " desc";
                case Quote.OrderArgument.OLDEST -> sqlOrder = QuoteDto.DATE_MODIFIED_COLUMN_NAME + " asc";
                case Quote.OrderArgument.ALPHABETICAL -> sqlOrder = QuoteDto.QUOTE_COLUMN_NAME + " asc";
                case Quote.OrderArgument.REVERSE_ALPHABETICAL -> sqlOrder = QuoteDto.QUOTE_COLUMN_NAME + " desc";
            }

            String sql = "SELECT * FROM " + QuoteDto.QUOTE_TABLE_NAME + " WHERE "
                    + QuoteDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild ";
            if (!chatTags.isEmpty()) {
                sql += "AND " + QuoteDto.ID_QUOTE_COLUMN_NAME + " IN :p_idsQuotes ";
            }
            sql += "ORDER BY " + sqlOrder + " LIMIT :p_resultsCount";

            NativeQuery<QuoteDto> query = session.createNativeQuery(sql, QuoteDto.class)
                    .setParameter("p_snowflakeGuild", event.getGuild().getId())
                    .setParameter("p_resultsCount", resultsCount);
            if (!chatTags.isEmpty()) {
                List<Long> idsQuotesToUse = QuoteTagDto.findByTags(chatTags, session).stream()
                        .map(QuoteTagDto::getIdQuote)
                        .toList();
                query.setParameter("p_idsQuotes", idsQuotesToUse);
            }
            List<QuoteDto> quotes = query.getResultList();

            if (quotes.isEmpty()) {
                processingContext.addMessages("No quotes found", ProcessingContext.MessageType.INFO_RESULT);
            } else {
                String message = Quote.processGetQuoteResult(quotes, chatCommand, session);
                processingContext.addMessages(message, ProcessingContext.MessageType.SUCCESS_RESULT);
            }
        } finally {
          transaction.commit();
          session.close();
        }
    }

    private void handleGetTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        Quote.OrderArgument chatOrder = chatCommand.getFirstArgumentAsEnum(Quote.ActionModifier.ORDER, Quote.OrderArgument.class, true, processingContext);
        long resultsCount = Quote.getCountModifierArgument(chatCommand, processingContext);

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            String sqlOrder = "";
            switch (chatOrder) {
                case Quote.OrderArgument.RANDOM -> sqlOrder = "rand()";
                case Quote.OrderArgument.NEWEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " desc";
                case Quote.OrderArgument.OLDEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " asc";
                case Quote.OrderArgument.ALPHABETICAL -> sqlOrder = TagDto.TAG_COLUMN_NAME + " asc";
                case Quote.OrderArgument.REVERSE_ALPHABETICAL -> sqlOrder = TagDto.TAG_COLUMN_NAME + " desc";
            }

            String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                    + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild ORDER BY "
                    + sqlOrder + " LIMIT :p_resultsCount";

            List<TagDto> tags = session.createNativeQuery(sql, TagDto.class)
                    .setParameter("p_snowflakeGuild", event.getGuild().getId())
                    .setParameter("p_resultsCount", resultsCount)
                    .getResultList();

            if (tags.isEmpty()) {
                processingContext.addMessages("No tags found", ProcessingContext.MessageType.INFO_RESULT);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                tags.forEach(tag -> stringBuilder.append("\"").append(tag.getTag()).append("\", "));
                stringBuilder.setLength(stringBuilder.length() - 2);
                processingContext.addMessages(stringBuilder.toString(), ProcessingContext.MessageType.SUCCESS_RESULT);
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private void handleNewQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        String chatNewQuote = chatCommand.getFirstArgument(Quote.ActionModifier.VALUE, false, true, processingContext)
                .getTrimmedUsedValue(processingContext);
        Check.isNotBlank(chatNewQuote, true, "New quote", null);
        Check.isInRange(chatNewQuote.length(), 1, Constants.QUOTE_MAX_LENGTH, true, "Quote length", null);

        Set<String> allTags = chatCommand.getArguments(Quote.ActionModifier.TAG, false, true, processingContext).stream()
                .map(argument -> argument.getTrimmedUsedValue(processingContext))
                .collect(Collectors.toSet());

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Check.isBooleanFalse(
                    QuoteDto.quoteExists(chatNewQuote, event.getGuild().getId(), session), true, "Quote " + chatNewQuote, "already exists"
            );

            List<String> nonExistentTags = TagDto.filterOutExistingTags(allTags, event.getGuild().getId(), session);
            Check.isEmpty(nonExistentTags, true, "Non existent tags");

            List<TagDto> tagDtos = TagDto.mapStringTagsToDtos(allTags, event.getGuild().getId(), session);
            QuoteDto newQuote = new QuoteDto(event.getAuthor().getId(), event.getGuild().getId(), chatNewQuote, tagDtos);
            List<QuoteDto.QuoteDistance> similarQuotes = QuoteDto.findSimilarQuotes(chatNewQuote, event.getGuild().getId(), session);

            Quote.tryToPersistEntity(newQuote, similarQuotes, session, event, chatCommand, processingContext);
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private void handleNewTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        String chatNewTag = chatCommand.getFirstArgument(Quote.ActionModifier.VALUE, false, true, processingContext)
                .getTrimmedNormalizedLowercaseUsedValue(processingContext);
        Check.isNotBlank(chatNewTag, true, "New tag", null);
        Check.isInRange(chatNewTag.length(), 1, Constants.TAG_MAX_LENGTH, true, "Tag length", null);

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Check.isBooleanFalse(
                    TagDto.tagExists(chatNewTag, event.getGuild().getId(),session), true, "Tag " + chatNewTag, "already exists"
            );

            TagDto newTag = new TagDto(event.getAuthor().getId(), event.getGuild().getId(), chatNewTag);
            List<TagDto.TagDistance> similarTags = TagDto.findSimilarTags(chatNewTag, event.getGuild().getId(), session);

            Quote.tryToPersistEntity(newTag, similarTags, session, event, chatCommand, processingContext);
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private static<T extends Persistable, U extends DtoWithDistance> void tryToPersistEntity(
            T persistable, List<U> similarEntities, Session session, MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext
    ) {
        boolean forceSwitchPresent = chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.FORCE);

        if (!similarEntities.isEmpty() && !forceSwitchPresent) {
            int timeToConfirmSeconds = ConfirmationMessageListener.addConfirmationMessageListener(
                    event, persistable, Constants.CONFIRMATION_ATTEMPTS
            );

            String message = Quote.getSimilarEntitiesWarningMessage(similarEntities, timeToConfirmSeconds, 5);
            processingContext.addMessages(message, ProcessingContext.MessageType.INFO_RESULT);
        } else {
            persistable.persist(processingContext, session);

            if (!similarEntities.isEmpty() && forceSwitchPresent) {
                int similarEntitiesCount = similarEntities.size();
                String entityName = similarEntities.getFirst().getName();

                String startOfMessage = similarEntitiesCount > 1
                        ? "Multiple similar " + entityName + "s (" + similarEntitiesCount + ") were detected"
                        : "Similar " + entityName + " was detected";

                processingContext.addMessages(
                        MessageFormat.format(
                                "{0}, action would normally require confirmation, however, since \"{1}\" switch modifier was used, the action was executed immediately",
                                startOfMessage,
                                ActionHandler.GlobalActionModifier.FORCE.toString()
                        ),
                        ProcessingContext.MessageType.FORCE_SWITCH_WARNING
                );
            }
        }
    }

    private static<T extends DtoWithDistance> String getSimilarEntitiesWarningMessage(
            List<T> similarEntities, int timeToConfirmSeconds, int maxDisplayedSimilarEntities
    ) {
        List<T> sortedSimilarEntities = similarEntities.stream()
                .sorted(Comparator.comparing(DtoWithDistance::getDistance))
                .limit(maxDisplayedSimilarEntities)
                .toList();

        String entityName = similarEntities.getFirst().getName();
        StringBuilder stringBuilder = new StringBuilder();
        sortedSimilarEntities.stream().limit(maxDisplayedSimilarEntities).forEach(dtoWithDistance ->
                stringBuilder.append("Similarity: ").append(Helper.formatDecimalNumber(100 - dtoWithDistance.getDistance() * 100, 2))
                        .append(" %, ").append(entityName).append(": ").append(dtoWithDistance.getValue()).append("\n")
        );

        if (similarEntities.size() == 1) {
            return MessageFormat.format("Similar {0} detected, confirm action in {1} seconds by replying \"{2}\" or \"{3}\":\n{4}",
                    entityName,
                    timeToConfirmSeconds,
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString(),
                    stringBuilder.toString()
            );
        } else if (similarEntities.size() <= maxDisplayedSimilarEntities) {
            return MessageFormat.format("Multiple similar {0} found ({1}), confirm action in {2} seconds by replying \"{3}\" or \"{4}\":\n{5}",
                    entityName + "s",
                    similarEntities.size(),
                    timeToConfirmSeconds,
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString(),
                    stringBuilder.toString()
            );
        } else {
            return MessageFormat.format("Multiple similar {0} found ({1}), confirm action in {2} seconds by replying \"{3}\" or \"{4}\", showing first {5} results:\n{6}",
                entityName + "s",
                similarEntities.size(),
                timeToConfirmSeconds,
                ChatConfirmation.Status.YES.toString(),
                ChatConfirmation.Status.NO.toString(),
                maxDisplayedSimilarEntities,
                stringBuilder.toString()
            );
        }
    }

    public enum ActionModifier { TYPE, TAG, ORDER, COUNT, VALUE }

    private enum TypeArgument { GET_QUOTE, GET_TAG, NEW_QUOTE, NEW_TAG }
    private enum OrderArgument { RANDOM, NEWEST, OLDEST, ALPHABETICAL, REVERSE_ALPHABETICAL }
    private enum CountArgument { ALL }
}
