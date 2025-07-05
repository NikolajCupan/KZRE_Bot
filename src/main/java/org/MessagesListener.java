package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.Action;
import org.action.ActionHandler;
import org.action.Quote;
import org.jetbrains.annotations.NotNull;
import org.parser.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MessagesListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesListener.class);

    private static final Map<String, ActionHandler> REGISTERED_ACTION_HANDLERS = new HashMap<>();
    private static final UserManager USER_MANAGER = new UserManager();
    private static final GuildManager GUILD_MANAGER = new GuildManager();

    static {
        MessagesListener.REGISTERED_ACTION_HANDLERS.put(Action.QUOTE.toString(), new Quote());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (Main.COMMAND_LINE_ARGUMENTS.contains(Constants.DEVELOPMENT_ARGUMENT)
                && !event.getGuild().getId().equals("949756585616474152")) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }

        String content = event.getMessage().getContentRaw();

        ChatCommand chatCommand = new ChatCommand(content, MessagesListener.REGISTERED_ACTION_HANDLERS);
        ActionHandler actionHandler = chatCommand.getAction();
        if (actionHandler == null) {
            return;
        }

        MessagesListener.LOGGER.info("Received action \"{}\"", content);

        MessagesListener.USER_MANAGER.refreshUser(event);
        MessagesListener.GUILD_MANAGER.refreshGuild(event);

        ProcessingContext processingContext = new ProcessingContext();
        actionHandler.executeAction(event, chatCommand, processingContext);

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);
        embedBuilder.addField("dummy", "dummy", false);

        if (processingContext.hasErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                    embedBuilder.addField(ProcessingContext.MessageType.ERROR.toString(), element.message(), false)
            );
        } else {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.RESULT, ProcessingContext.MessageType.SUCCESS, ProcessingContext.MessageType.WARNING)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }

        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
