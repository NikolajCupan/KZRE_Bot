package org.listener;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.Main;
import org.database.dto.GuildDto;
import org.database.dto.UserDto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.utility.Constants;
import org.utility.ProcessingContext;
import org.utility.Request;

import java.awt.*;
import java.util.List;

public abstract class MessageListener extends ListenerAdapter {
    public static void returnResponse(MessageChannel channel, String result) {
        if (result != null && !result.isBlank()) {
            channel.sendMessage(result).queue();
        }
    }

    protected static void returnResponseVerbose(MessageChannel channel, EmbedBuilder result) {
        if (!result.isEmpty()) {
            channel.sendMessageEmbeds(result.build()).queue();
        }
    }

    private static void beforeMessageProcessed(MessageReceivedEvent event) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            GuildDto.refreshGuild(event.getGuild().getId(), session);
            UserDto.refreshUser(event.getAuthor().getId(), session);
        } finally {
            transaction.commit();
            session.close();
        }
    }

    private static String processRequestResult(ProcessingContext processingContext) {
        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            return processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR))
                    .getFirst().message();
        } else if (processingContext.hasMessageOfType(ProcessingContext.MessageType.ERROR)) {
            return processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR))
                    .getFirst().message();
        } else {
            List<ProcessingContext.Message> messages = processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT));
            if (!messages.isEmpty()) {
                return messages.getFirst().message();
            }
        }

        return null;
    }

    private static EmbedBuilder processRequestResultVerbose(ProcessingContext processingContext) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);

        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
            return embedBuilder;
        } else if (processingContext.hasMessageOfType(ProcessingContext.MessageType.ERROR)) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
            return embedBuilder;
        }


        processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT)).forEach(element ->
                embedBuilder.addField(element.messageType().toString(), element.message(), false)
        );

        processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_WARNING, ProcessingContext.MessageType.WARNING, ProcessingContext.MessageType.FORCE_SWITCH_WARNING)).forEach(element ->
                embedBuilder.addField(element.messageType().toString(), element.message(), false)
        );

        return embedBuilder;
    }

    protected abstract boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (Main.COMMAND_LINE_ARGUMENTS.contains(Constants.DEVELOPMENT_ARGUMENT)
                && !event.getGuild().getId().equals(Constants.DEVELOPMENT_SERVER_ID)) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }


        ProcessingContext processingContext = new ProcessingContext();
        Request request = new Request(event.getChannel().getId(), event.getAuthor().getId(), event.getGuild().getId());
        boolean verboseResponse = false;

        Object lock = request.acquireLock();
        if (lock != null) {
            synchronized (lock) {
                try {
                    MessageListener.beforeMessageProcessed(event);
                    verboseResponse = this.processMessage(event, processingContext);
                } finally {
                    request.releaseLock();
                }
            }
        } else {
            processingContext.addMessages("Request could not be processed", ProcessingContext.MessageType.ERROR);
        }

        if (verboseResponse) {
            EmbedBuilder response = MessageListener.processRequestResultVerbose(processingContext);
            MessageListener.returnResponseVerbose(event.getChannel(), response);
        } else {
            String response = MessageListener.processRequestResult(processingContext);
            MessageListener.returnResponse(event.getChannel(), response);
        }
    }
}
