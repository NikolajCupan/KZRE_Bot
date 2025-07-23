package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.*;
import org.database.dto.QuoteDto;
import org.database.dto.TagDto;
import org.exception.CustomException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.javatuples.Pair;
import org.parsing.ChatCommand;
import org.parsing.ChatConfirmation;
import org.parsing.Modifier;
import org.utility.*;

import java.text.MessageFormat;
import java.util.*;

public class Quote extends ActionHandler {
    static {
        ActionHandler.ACTION_MODIFIERS.put(
                ActionModifier.TYPE,
                new Modifier<>(TypeArgument.class, null, false, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                ActionModifier.TAG,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                ActionModifier.ORDER,
                new Modifier<>(OrderArgument.class, OrderArgument.NEWEST, false, false, false, false, null, null)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                ActionModifier.COUNT,
                new Modifier<>(CountArgument.class, 5L, false, false, true, false, 1L, Long.MAX_VALUE)
        );
        ActionHandler.ACTION_MODIFIERS.put(
                ActionModifier.VALUE,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        try {
            TypeArgument typeArgument = chatCommand.getFirstArgumentAsEnum(ActionModifier.TYPE, TypeArgument.class, true, processingContext);

            switch (typeArgument) {
                case TypeArgument.GET_QUOTE -> this.handleGetQuote(event, chatCommand, processingContext);
                case TypeArgument.GET_TAG -> this.handleGetTag(event, chatCommand, processingContext);
                case TypeArgument.NEW_QUOTE -> this.handleNewQuote(event, chatCommand, processingContext);
                case TypeArgument.NEW_TAG -> this.handleNewTag(event, chatCommand, processingContext);
            }
        } catch (CustomException exception) {
            processingContext.addMessages(exception.getMessage(), ProcessingContext.MessageType.ERROR);
        }
    }

    @Override
    protected Class<? extends Enum<?>> getModifierEnumClass() {
        return ActionModifier.class;
    }

    private void handleGetQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {}

    private void handleGetTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        OrderArgument chatOrder = chatCommand.getFirstArgumentAsEnum(ActionModifier.ORDER, OrderArgument.class, true, processingContext);
        TypedValue chatCount = chatCommand.getFirstArgument(ActionModifier.COUNT, false, true, processingContext);

        long resultsCount = Long.MAX_VALUE;
        if (chatCount.getType() == TypedValue.Type.WHOLE_NUMBER) {
            resultsCount = Long.parseLong(chatCount.getUsedValue());
        }


        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            String sqlOrder = "";
            switch (chatOrder) {
                case OrderArgument.RANDOM -> sqlOrder = "rand()";
                case OrderArgument.NEWEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " desc";
                case OrderArgument.OLDEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " asc";
                case OrderArgument.ALPHABETICAL -> sqlOrder = TagDto.TAG_COLUMN_NAME + " asc";
                case OrderArgument.REVERSE_ALPHABETICAL -> sqlOrder = TagDto.TAG_COLUMN_NAME + " desc";
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
        String chatNewQuote = chatCommand.getFirstArgument(ActionModifier.VALUE, false, true, processingContext)
                .getTrimmedUsedValue(processingContext);
        Check.isNotBlank(chatNewQuote, true, "New quote", null);
        Check.isInRange(chatNewQuote.length(), 1, Constants.QUOTE_MAX_LENGTH, true, "Quote length", null);

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            Check.isBooleanFalse(
                    QuoteDto.quoteExists(chatNewQuote, event.getGuild().getId(), session), true, "Quote " + chatNewQuote, "already exists"
            );
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private void handleNewTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        String chatNewTag = chatCommand.getFirstArgument(ActionModifier.VALUE, false, true, processingContext)
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
            List<Pair<Double, TagDto>> similarTags = TagDto.findSimilarTags(chatNewTag, event.getGuild().getId(), session);

            boolean forceSwitchPresent = chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.FORCE);
            if (!similarTags.isEmpty() && !forceSwitchPresent) {
                int timeToConfirmSeconds = ConfirmationMessageListener.addConfirmationMessageListener(
                        event, newTag, Constants.CONFIRMATION_ATTEMPTS
                );

                String message = Quote.getSimilarTagsWarning(similarTags, timeToConfirmSeconds);
                processingContext.addMessages(message, ProcessingContext.MessageType.INFO_RESULT);
            } else {
                newTag.persist(processingContext, session);

                if (!similarTags.isEmpty() && forceSwitchPresent) {
                    processingContext.addMessages(
                            MessageFormat.format(
                                    "Similar tag(s) were detected, action would normally require confirmation, however, since \"{0}\" switch modifier was used, the action was executed immediately",
                                    ActionHandler.GlobalActionModifier.FORCE.toString()
                            ),
                            ProcessingContext.MessageType.FORCE_SWITCH_WARNING
                    );
                }
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private static String getSimilarTagsWarning(List<Pair<Double, TagDto>> similarTags, int timeToConfirmSeconds) {
        int maxDisplayedSimilarTags = 5;
        List<Pair<Double, TagDto>> sortedSimilarTags = similarTags.stream()
                .sorted(Comparator.comparing(Pair::getValue0))
                .limit(maxDisplayedSimilarTags)
                .toList();

        StringBuilder stringBuilder = new StringBuilder();
        sortedSimilarTags.stream().limit(maxDisplayedSimilarTags).forEach(pair ->
                stringBuilder.append("Similarity: ").append(Helper.formatDecimalNumber(100 - pair.getValue0() * 100, 2))
                        .append(" %, tag: ").append(pair.getValue1().getTag()).append("\n")
        );

        if (similarTags.size() == 1) {
            return MessageFormat.format("Similar tag detected, confirm action in {0} seconds by replying \"{1}\" or \"{2}\":\n{3}",
                    timeToConfirmSeconds,
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString(),
                    stringBuilder.toString()
            );
        } else if (similarTags.size() <= maxDisplayedSimilarTags) {
            return MessageFormat.format("Multiple similar tags found ({0}), confirm action in {1} seconds by replying \"{2}\" or \"{3}\":\n{4}",
                    similarTags.size(),
                    timeToConfirmSeconds,
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString(),
                    stringBuilder.toString()
            );
        } else {
            return MessageFormat.format("Multiple similar tags found ({0}), confirm action in {1} seconds by replying \"{2}\" or \"{3}\", showing first {4} results:\n{5}",
                similarTags.size(),
                timeToConfirmSeconds,
                ChatConfirmation.Status.YES.toString(),
                ChatConfirmation.Status.NO.toString(),
                maxDisplayedSimilarTags,
                stringBuilder.toString()
            );
        }
    }

    public enum ActionModifier { TYPE, TAG, ORDER, COUNT, VALUE }

    private enum TypeArgument { GET_QUOTE, GET_TAG, NEW_QUOTE, NEW_TAG }
    private enum OrderArgument { RANDOM, NEWEST, OLDEST, ALPHABETICAL, REVERSE_ALPHABETICAL }
    private enum CountArgument { ALL }
}
