package org;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.Action;
import org.action.ActionHandler;
import org.action.Quote;
import org.jetbrains.annotations.NotNull;
import org.parser.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MessagesListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessagesListener.class);
    private static final Map<String, ActionHandler> REGISTERED_ACTION_HANDLERS = new HashMap<>();;

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

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (!content.startsWith(ChatCommand.ACTION_PREFIX)) {
            return;
        }

        ChatCommand chatCommand = new ChatCommand(content);
        ActionHandler actionHandler = MessagesListener.REGISTERED_ACTION_HANDLERS.get(chatCommand.getChatAction());
        if (actionHandler == null) {
            return;
        }

        MessagesListener.LOGGER.info("Received action \"{}\"", content);
        actionHandler.executeAction(event, chatCommand);
    }
}
