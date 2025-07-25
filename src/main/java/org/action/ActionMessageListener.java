package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.MessageListener;
import org.parsing.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.Helper;
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

        for (Enum<?> modifierName : modifiers.keySet()) {
            List<TypedValue> arguments = modifiers.get(modifierName);
            List<TypedValue> unusedArguments = arguments.stream()
                    .filter(element -> !element.getUsed()
                            && element.getResolution() != TypedValue.Resolution.MODIFIER_MISSING
                            && element.getResolution() != TypedValue.Resolution.ARGUMENT_MISSING)
                    .toList();

            boolean modifierAccessed = accessedModifiers.contains(modifierName);
            boolean modifierAddedAfterParsing = addedAfterParsingModifiers.contains(modifierName);
            boolean modifierIsSwitch = arguments.isEmpty() || arguments.getFirst().getType() == TypedValue.Type.SWITCH;

            if (!modifierAccessed) {
                if (!modifierAddedAfterParsing && !unusedArguments.isEmpty()) {
                    if (modifierIsSwitch) {
                        processingContext.addMessages(
                                MessageFormat.format(
                                        "Modifier \"{0}\" and its arguments {1} were not used, additionally \"{0}\" is a switch modifier and should not be provided with any arguments",
                                        modifierName,
                                        Helper.stringifyCollection(unusedArguments)
                                ),
                                ProcessingContext.MessageType.WARNING
                        );
                    } else {
                        processingContext.addMessages(
                                MessageFormat.format(
                                        "Modifier \"{0}\" and its arguments {1} were not used",
                                        modifierName,
                                        Helper.stringifyCollection(unusedArguments)
                                ),
                                ProcessingContext.MessageType.WARNING
                        );
                    }
                } else if (!modifierAddedAfterParsing) {
                    processingContext.addMessages(
                            MessageFormat.format("{0} \"{1}\" was not used", modifierIsSwitch ? "Switch modifier" : "Modifier", modifierName),
                            ProcessingContext.MessageType.WARNING
                    );
                }
            } else if (!unusedArguments.isEmpty()) {
                if (modifierIsSwitch) {
                    processingContext.addMessages(
                            MessageFormat.format(
                                    "Modifier \"{0}\" is a switch modifier, it should not be provided with any arguments, arguments were ignored: {1}",
                                    modifierName,
                                    Helper.stringifyCollection(unusedArguments)
                            ),
                            ProcessingContext.MessageType.WARNING
                    );
                } else {
                    processingContext.addMessages(
                            MessageFormat.format(
                                    "Not all arguments for modifier \"{0}\" were used, the unused arguments are: {1}",
                                    modifierName,
                                    Helper.stringifyCollection(unusedArguments)
                            ),
                            ProcessingContext.MessageType.WARNING
                    );
                }
            }
        }
    }

    private static void warnIfForceWasNotNeeded(ChatCommand chatCommand, ProcessingContext processingContext) {
        boolean forceSwitchAccessed = chatCommand.getModifierMap().getAccessedModifiers().contains(
                ActionHandler.GlobalActionModifier.FORCE
        );
        boolean hasForceSwitchWarning = processingContext.hasMessageOfType(ProcessingContext.MessageType.FORCE_SWITCH_WARNING);

        if (forceSwitchAccessed && !hasForceSwitchWarning) {
            processingContext.addMessages(
                    MessageFormat.format(
                            "All validation checks passed successfully, the \"{0}\" switch modifier was provided but not needed",
                            ActionHandler.GlobalActionModifier.FORCE.toString()
                    ),
                    ProcessingContext.MessageType.WARNING
            );
        }
    }

    @Override
    public boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext) {
        if (ConfirmationMessageListener.confirmationKeyExists(event.getChannel().getId(), event.getAuthor().getId())) {
            // Waiting for confirmation
            ActionMessageListener.LOGGER.info("Message ignored because there is a pending confirmation");
            return false;
        }

        ChatCommand chatCommand = new ChatCommand(event.getMessage(), ActionMessageListener.REGISTERED_ACTION_HANDLERS, processingContext);
        ActionHandler actionHandler = chatCommand.getActionHandler();
        if (actionHandler == null || processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            return false;
        }

        String guildUserLockedChannel =
                ConfirmationMessageListener.getGuildUserLockedChannel(event.getAuthor().getId(), event.getGuild().getId());
        if (guildUserLockedChannel != null) {
            processingContext.addMessages(
                    MessageFormat.format("Please confirm the pending action in channel \"{0}\" before running another command", guildUserLockedChannel),
                    ProcessingContext.MessageType.ERROR
            );
            return false;
        }


        ActionMessageListener.LOGGER.info("Received action \"{}\"", event.getMessage().getContentRaw());

        actionHandler.executeAction(event, chatCommand, processingContext);

        boolean verboseSwitchPresent = chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.VERBOSE);
        ActionMessageListener.warnIfUnusedModifiersOrArgumentsExist(chatCommand, processingContext);
        ActionMessageListener.warnIfForceWasNotNeeded(chatCommand, processingContext);

        return verboseSwitchPresent;
    }
}
