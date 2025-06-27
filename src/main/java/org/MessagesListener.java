package org;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.command.CommandHandler;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MessagesListener extends ListenerAdapter {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MessagesListener.class);
    private static final Map<String, CommandHandler> REGISTERED_COMMAND_HANDLERS = new HashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();

        if (!content.startsWith(Constants.COMMAND_PREFIX)) {
            return;
        }

        String[] tokens = content.split(" ");
        String command = tokens[0].substring(1);
        CommandHandler commandHandler = MessagesListener.REGISTERED_COMMAND_HANDLERS.get(command);
        if (commandHandler == null) {
            return;
        }

        MessagesListener.LOGGER.info("Received command \"{}\"", content);
        commandHandler.executeAction(event);
    }

    public static void registerCommandHandler(CommandHandler commandHandler) {
        if (commandHandler.getCommand().contains(" ")) {
            MessagesListener.LOGGER.error("Command \"{}\" contains space", commandHandler);
            return;
        }

        if (MessagesListener.REGISTERED_COMMAND_HANDLERS.containsKey(commandHandler.getCommand())) {
            MessagesListener.LOGGER.error("Command \"{}\" is already registered", commandHandler);
            return;
        }

        MessagesListener.REGISTERED_COMMAND_HANDLERS.put(commandHandler.getCommand(), commandHandler);
    }
}
