package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Helper;
import org.Main;
import org.Modifier;
import org.ProcessingContext;
import org.dto.TagDto;
import org.exception.CustomException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.parser.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;

public class Quote extends ActionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Quote.class);

    private enum ActionModifier { TYPE, TAG, ORDER, COUNT, VALUE }

    private enum TypeArgument { GET_QUOTE, GET_TAG, NEW_QUOTE, NEW_TAG }
    private enum OrderArgument { RANDOM, NEWEST, OLDEST }
    private enum CountArgument { ALL }

    private static final Action action = Action.QUOTE;
    private static final Map<ActionModifier, Modifier<? extends Enum<?>, ? extends Enum<?>>> ACTION_MODIFIERS =
            new EnumMap<>(ActionModifier.class);

    static {
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TYPE,
                new Modifier<>(ActionModifier.TYPE, TypeArgument.class, TypeArgument.GET_QUOTE, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TAG,
                new Modifier<>(ActionModifier.TAG, Helper.EmptyEnum.class, null, true, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.ORDER,
                new Modifier<>(ActionModifier.ORDER, OrderArgument.class, OrderArgument.RANDOM, false, false, false)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.COUNT,
                new Modifier<>(ActionModifier.COUNT, CountArgument.class, 5, false, false, true)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.VALUE,
                new Modifier<>(ActionModifier.VALUE, Helper.EmptyEnum.class, null, true, false, false)
        );
    }

    @Override
    public Action getAction() {
        return Quote.action;
    }

    @Override
    public Set<String> getActionModifiersKeywords() {
        Set<String> keywords = new HashSet<>();
        for (ActionModifier modifier : ActionModifier.class.getEnumConstants()) {
            keywords.add(modifier.toString());
        }

        return keywords;
    }

    @Override
    public void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        TypeArgument typeArgument = chatCommand.getArgumentAsEnum(Quote.ACTION_MODIFIERS.get(ActionModifier.TYPE), TypeArgument.class);
        try {
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

    private void handleGetQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
    }

    private void handleGetTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        OrderArgument chatOrder = chatCommand.getArgumentAsEnum(Quote.ACTION_MODIFIERS.get(ActionModifier.ORDER), OrderArgument.class);
        Helper.TypedValue chatCount = chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.COUNT));

        long resultsCount = Long.MAX_VALUE;
        if (chatCount.type() == Helper.TypedValue.Type.WHOLE_NUMBER) {
            resultsCount = Long.parseLong(chatCount.value());
        }

        Helper.failIfOutOfRange(resultsCount, 0, Long.MAX_VALUE, MessageFormat.format(
                "Argument for modifier \"{0}\" cannot be negative", ActionModifier.COUNT
        ));


        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            String sqlOrder = "";
            switch (chatOrder) {
                case OrderArgument.RANDOM -> sqlOrder = "rand()";
                case OrderArgument.NEWEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " desc";
                case OrderArgument.OLDEST -> sqlOrder = TagDto.DATE_MODIFIED_COLUMN_NAME + " asc";
            }

            String sql = "SELECT * FROM " + TagDto.TAG_TABLE_NAME + " WHERE "
                    + TagDto.SNOWFLAKE_GUILD_COLUMN_NAME + " = :p_snowflakeGuild ORDER BY "
                    + sqlOrder + " LIMIT :p_resultsCount";

            List<TagDto> tags = session.createNativeQuery(sql, TagDto.class)
                    .setParameter("p_snowflakeGuild", event.getMessage().getGuild().getId())
                    .setParameter("p_resultsCount", resultsCount)
                    .getResultList();

            if (tags.isEmpty()) {
                processingContext.addMessages("No tags found", ProcessingContext.MessageType.WARNING);
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                tags.forEach(tag -> stringBuilder.append(tag.getTag()).append(' '));
                processingContext.addMessages(stringBuilder.toString(), ProcessingContext.MessageType.RESULT);
            }
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private void handleNewQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
    }

    private void handleNewTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        Helper.TypedValue chatNewTag = chatCommand.getArgument(Quote.ACTION_MODIFIERS.get(ActionModifier.VALUE));
        Helper.failIfBlank(chatNewTag.value(), MessageFormat.format("Argument for modifier \"{0}\" was not found", ActionModifier.VALUE));


        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        String chatNewTagFirstWord = chatNewTag.valueFirstWord();
        try {
            if (!chatNewTagFirstWord.equals(chatNewTag.value())) {
                processingContext.addMessages(
                        MessageFormat.format("Argument for modifier \"{0}\" should not contain spaces, everything after first word was ignored", ActionModifier.VALUE),
                        ProcessingContext.MessageType.WARNING
                );
            }

            TagDto newTag = new TagDto(event.getAuthor().getId(), event.getGuild().getId(), chatNewTagFirstWord);
            session.persist(newTag);

            processingContext.addMessages(
                    MessageFormat.format("New tag \"{0}\" was successfully created", chatNewTagFirstWord),
                    ProcessingContext.MessageType.SUCCESS
            );
        } catch (ConstraintViolationException exception) {
            processingContext.addMessages(
                    MessageFormat.format("Tag \"{0}\" already exists", chatNewTagFirstWord),
                    ProcessingContext.MessageType.ERROR
            );
        } finally {
            transaction.commit();
            session.close();
        }
    }
}
