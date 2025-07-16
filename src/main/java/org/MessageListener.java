package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.ConfirmationMessageListener;
import org.database.GuildManager;
import org.database.Persistable;
import org.database.UserManager;
import org.jetbrains.annotations.NotNull;
import org.utility.Constants;
import org.utility.ProcessingContext;

import java.awt.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class MessageListener extends ListenerAdapter {
    private static final UserManager USER_MANAGER;
    private static final GuildManager GUILD_MANAGER;
    private static final Set<ConfirmationKey> PENDING_CONFIRMATIONS;

    static {
        USER_MANAGER = new UserManager();
        GUILD_MANAGER = new GuildManager();
        PENDING_CONFIRMATIONS = Collections.synchronizedSet(new HashSet<>());
    }

    public static void addConfirmationMessageListener(MessageReceivedEvent event, Persistable objectToStore) {
        ConfirmationKey confirmationKey = new ConfirmationKey(event.getChannel().getId(), event.getAuthor().getId());
        if (MessageListener.PENDING_CONFIRMATIONS.contains(confirmationKey)) {
            throw new RuntimeException("The confirmation key is already present in the set");
        }

        MessageListener.PENDING_CONFIRMATIONS.add(confirmationKey);
        Main.JDA_API.addEventListener(new ConfirmationMessageListener(event.getChannel().getId(), event.getAuthor().getId(), objectToStore));
    }

    public static void removeConfirmationMessageListener(ConfirmationMessageListener messageListener) {
        ConfirmationKey confirmationKey = new ConfirmationKey(messageListener.getChannelId(), messageListener.getUserId());
        if (!MessageListener.PENDING_CONFIRMATIONS.contains(confirmationKey)) {
            throw new RuntimeException("The confirmation key is not present in the set");
        }

        MessageListener.PENDING_CONFIRMATIONS.remove(confirmationKey);
        Main.JDA_API.removeEventListener(messageListener);
    }

    public static boolean confirmationKeyExists(String channelId, String userId) {
        return MessageListener.PENDING_CONFIRMATIONS.contains(new ConfirmationKey(channelId, userId));
    }

    private static void beforeMessageProcessed(MessageReceivedEvent event) {
        MessageListener.USER_MANAGER.refreshUser(event);
        MessageListener.GUILD_MANAGER.refreshGuild(event);
    }

    private static void afterMessageProcessed(
            MessageReceivedEvent event, EmbedBuilder embedBuilder, ProcessingContext processingContext, boolean verboseResponse
    ) {
        if (processingContext.hasParsingErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        } else if (processingContext.hasErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        } else {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }

        if (verboseResponse) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_WARNING, ProcessingContext.MessageType.WARNING)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }


        if (!embedBuilder.isEmpty()) {
            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        }
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
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);

        MessageListener.beforeMessageProcessed(event);
        boolean verboseResponse = this.processMessage(event, processingContext);
        MessageListener.afterMessageProcessed(event, embedBuilder, processingContext, verboseResponse);
    }

    private record ConfirmationKey(String channelId, String userId) {}
}
