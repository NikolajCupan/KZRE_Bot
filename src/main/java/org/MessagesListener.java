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

    private final Map<String, ActionHandler> registeredActionHandlers;

    public MessagesListener() {
        this.registeredActionHandlers = new HashMap<>();
    }

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
        ActionHandler actionHandler = this.registeredActionHandlers.get(command.getAction());
        if (actionHandler == null) {
            return;
        }

        MessagesListener.LOGGER.info("Received action \"{}\"", content);
        actionHandler.executeAction(event, command);
    }

    public void registerActionHandler(ActionHandler actionHandler) {
        if (actionHandler.getAction().contains(" ")) {
            MessagesListener.LOGGER.error("Action \"{}\" contains space", actionHandler);
            return;
        }

        if (this.registeredActionHandlers.containsKey(actionHandler.getAction())) {
            MessagesListener.LOGGER.error("Action \"{}\" is already registered", actionHandler);
            return;
        }

        this.registeredActionHandlers.put(actionHandler.getAction(), actionHandler);
    }
}
