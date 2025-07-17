package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.MessageListener;
import org.parsing.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.ProcessingContext;
import org.utility.TypedValue;

import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class ActionMessageListener extends MessageListener {
    private static final Logger LOGGER;

    private static final Map<String, ActionHandler> REGISTERED_ACTION_HANDLERS;

    static {
        LOGGER = LoggerFactory.getLogger(ActionMessageListener.class);

        REGISTERED_ACTION_HANDLERS = new HashMap<>();
        ActionMessageListener.REGISTERED_ACTION_HANDLERS.put(Action.QUOTE.toString().toUpperCase(), new Quote());
    }

    private static void warnIfUnusedModifiersOrArgumentsExist(ChatCommand chatCommand, ProcessingContext processingContext) {
        Map<Enum<?>, List<TypedValue>> modifiers = chatCommand.getModifierMap().getModifiers();
        Set<Enum<?>> accessedModifiers = chatCommand.getModifierMap().getAccessedModifiers();
        Set<Enum<?>> addedAfterParsingModifiers = chatCommand.getModifierMap().getAddedAfterParsingModifiers();

        for (Enum<?> key : modifiers.keySet()) {
            List<TypedValue> arguments = modifiers.get(key);
            List<TypedValue> unusedArguments = arguments.stream()
                    .filter(element -> !element.getUsed()
                            && element.getResolution() != TypedValue.Resolution.MODIFIER_MISSING
                            && element.getResolution() != TypedValue.Resolution.ARGUMENT_MISSING)
                    .toList();

            boolean modifierAccessed = accessedModifiers.contains(key)
                    || key.toString().equalsIgnoreCase(ActionHandler.GlobalActionModifier.VERBOSE.toString());
            boolean modifierAddedAfterParsing = addedAfterParsingModifiers.contains(key);
            boolean modifierIsSwitch = arguments.isEmpty() || arguments.getFirst().getType() == TypedValue.Type.SWITCH;

            StringBuilder stringBuilder = new StringBuilder();
            if (!unusedArguments.isEmpty()) {
                unusedArguments.forEach(element -> stringBuilder.append("\"").append(element.getRawValue()).append("\", "));
                stringBuilder.setLength(stringBuilder.length() - 2);
            }

            if (!modifierAccessed) {
                if (!modifierAddedAfterParsing && !unusedArguments.isEmpty()) {
                    if (modifierIsSwitch) {
                        processingContext.addMessages(
                                MessageFormat.format(
                                        "Modifier \"{0}\" and its arguments [{1}] were not used, additionally \"{0}\" is a switch modifier and should not be provided with any arguments",
                                        key,
                                        stringBuilder.toString()
                                ),
                                ProcessingContext.MessageType.WARNING
                        );
                    } else {
                        processingContext.addMessages(
                                MessageFormat.format("Modifier \"{0}\" and its arguments [{1}] were not used", key, stringBuilder.toString()),
                                ProcessingContext.MessageType.WARNING
                        );
                    }
                } else if (!modifierAddedAfterParsing) {
                    processingContext.addMessages(
                            MessageFormat.format("Modifier \"{0}\" was not used", key),
                            ProcessingContext.MessageType.WARNING
                    );
                }
            } else if (!unusedArguments.isEmpty()) {
                if (modifierIsSwitch) {
                    processingContext.addMessages(
                            MessageFormat.format("Modifier \"{0}\" is a switch modifier, it should not be provided with any arguments, arguments were ignored: [{1}]", key, stringBuilder.toString()),
                            ProcessingContext.MessageType.WARNING
                    );
                } else {
                    processingContext.addMessages(
                            MessageFormat.format("Not all arguments for modifier \"{0}\" were used, the unused arguments are: [{1}]", key, stringBuilder.toString()),
                            ProcessingContext.MessageType.WARNING
                    );
                }
            }
        }
    }

    @Override
    public boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext) {
        if (ConfirmationMessageListener.confirmationKeyExists(event.getChannel().getId(), event.getAuthor().getId())) {
            // Waiting for confirmation
            ActionMessageListener.LOGGER.info("Message ignored because it is a pending confirmation");
            return false;
        }

        ChatCommand chatCommand = new ChatCommand(event.getMessage(), ActionMessageListener.REGISTERED_ACTION_HANDLERS, processingContext);
        ActionHandler actionHandler = chatCommand.getActionHandler();
        if (actionHandler == null || processingContext.hasParsingErrorMessage()) {
            return false;
        }


        ActionMessageListener.LOGGER.info("Received action \"{}\"", event.getMessage().getContentRaw());

        actionHandler.executeAction(event, chatCommand, processingContext);
        ActionMessageListener.warnIfUnusedModifiersOrArgumentsExist(chatCommand, processingContext);

        return chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.VERBOSE);
    }
}
