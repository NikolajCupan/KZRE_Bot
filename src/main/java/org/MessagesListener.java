package org;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.ActionHandler;
import org.parser.Command;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MessagesListener extends ListenerAdapter {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MessagesListener.class);
    private static final Map<String, ActionHandler> REGISTERED_ACTION_HANDLERS = new HashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (!content.startsWith(Command.ACTION_PREFIX)) {
            return;
        }

        Command command = new Command(content);
        ActionHandler actionHandler = MessagesListener.REGISTERED_ACTION_HANDLERS.get(command.getAction());
        if (actionHandler == null) {
            return;
        }

        MessagesListener.LOGGER.info("Received action \"{}\"", content);
        actionHandler.executeAction(event, command);
    }

    public static void registerActionHandler(ActionHandler actionHandler) {
        if (actionHandler.getAction().contains(" ")) {
            MessagesListener.LOGGER.error("Action \"{}\" contains space", actionHandler);
            return;
        }

        if (MessagesListener.REGISTERED_ACTION_HANDLERS.containsKey(actionHandler.getAction())) {
            MessagesListener.LOGGER.error("Action \"{}\" is already registered", actionHandler);
            return;
        }

        MessagesListener.REGISTERED_ACTION_HANDLERS.put(actionHandler.getAction(), actionHandler);
    }
}
