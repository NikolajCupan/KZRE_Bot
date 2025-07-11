package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.*;
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

    public enum ActionModifier { TYPE, TAG, ORDER, COUNT, VALUE }

    private enum TypeArgument { GET_QUOTE, GET_TAG, NEW_QUOTE, NEW_TAG }
    private enum OrderArgument { RANDOM, NEWEST, OLDEST }
    private enum CountArgument { ALL }

    private static final Action action = Action.QUOTE;
    private static final Map<ActionModifier, Modifier<? extends Enum<?>, ? extends Number>> ACTION_MODIFIERS =
            new EnumMap<>(ActionModifier.class);

    static {
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TYPE,
                new Modifier<>(TypeArgument.class, null, false, false, false, false, null, null)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.TAG,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.ORDER,
                new Modifier<>(OrderArgument.class, OrderArgument.RANDOM, false, false, false, false, null, null)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.COUNT,
                new Modifier<>(CountArgument.class, 5L, false, false, true, false, 1L, Long.MAX_VALUE)
        );
        Quote.ACTION_MODIFIERS.put(
                ActionModifier.VALUE,
                new Modifier<>(Helper.EmptyEnum.class, null, true, false, false, false, null, null)
        );
    }

    @Override
    public Action getAction() {
        return Quote.action;
    }

    @Override
    public Enum<?> getActionModifierEnumerator(String strModifier) {
        return Enum.valueOf(ActionModifier.class, strModifier);
    }

    @Override
    public Modifier<? extends Enum<?>, ? extends Number> getModifier(Enum<?> actionModifier) {
        return Quote.ACTION_MODIFIERS.get(actionModifier);
    }

    @Override
    public Class<? extends Enum<?>> getActionModifierEnum() {
        return ActionModifier.class;
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

    private void handleGetQuote(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
    }

    private void handleGetTag(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext) {
        OrderArgument chatOrder = chatCommand.getFirstArgumentAsEnum(ActionModifier.ORDER, OrderArgument.class, true, processingContext);
        Helper.TypedValue chatCount = chatCommand.getFirstArgument(ActionModifier.COUNT, false, true, processingContext);

        long resultsCount = Long.MAX_VALUE;
        if (chatCount.getType() == Helper.TypedValue.Type.WHOLE_NUMBER) {
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
        Helper.TypedValue chatNewTag = chatCommand.getFirstArgument(ActionModifier.VALUE, false, true, processingContext);

        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            TagDto newTag = new TagDto(event.getAuthor().getId(), event.getGuild().getId(), chatNewTag.getUsedValue());
            session.persist(newTag);

            processingContext.addMessages(
                    MessageFormat.format("New tag \"{0}\" was successfully created", chatNewTag.getUsedValue()),
                    ProcessingContext.MessageType.SUCCESS
            );
        } catch (ConstraintViolationException exception) {
            processingContext.addMessages(
                    MessageFormat.format("Tag \"{0}\" already exists", chatNewTag.getUsedValue()),
                    ProcessingContext.MessageType.ERROR
            );
        } finally {
            transaction.commit();
            session.close();
        }
    }
}
