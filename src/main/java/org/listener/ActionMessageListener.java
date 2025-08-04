package org.listener;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.action.Action;
import org.action.ActionHandler;
import org.action.Index;
import org.action.Quote;
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

        REGISTERED_ACTION_HANDLERS = Map.of(
                Action.INDEX.toString().toUpperCase(), new Index(),
                Action.QUOTE.toString().toUpperCase(), new Quote()
        );
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

            if (modifierAccessed && unusedArguments.isEmpty()) {
                // no warnings needed
                assert !modifierIsSwitch || arguments.isEmpty();
                continue;
            } else if (!modifierAccessed && modifierAddedAfterParsing) {
                // no warnings needed
                assert unusedArguments.isEmpty();
                continue;
            }


            String modifierTypeName = modifierIsSwitch ? "Switch modifier" : "Modifier";
            String warningMessage;
            if (!modifierAccessed) {
                if (unusedArguments.isEmpty()) {
                    warningMessage = MessageFormat.format(
                            "{0} \"{1}\" was not used", modifierTypeName, modifierName
                    );
                } else {
                    warningMessage = MessageFormat.format(
                            "{0} \"{1}\" and its arguments {2} were not used",
                            modifierTypeName,
                            modifierName,
                            Helper.stringifyCollection(unusedArguments)
                    );

                    if (modifierIsSwitch) {
                        warningMessage += ", additionally switch modifiers should not be provided with any arguments";
                    }
                }
            } else {
                if (modifierIsSwitch) {
                    assert unusedArguments.size() == arguments.size();
                    warningMessage = MessageFormat.format(
                            "{0} \"{1}\" should not be provided with any arguments, all arguments were ignored: {2}",
                            modifierTypeName,
                            modifierName,
                            Helper.stringifyCollection(unusedArguments)
                    );
                } else {
                    warningMessage = MessageFormat.format(
                            "{0} \"{1}\" did not use all arguments, the unused arguments are: {2}",
                            modifierTypeName,
                            modifierName,
                            Helper.stringifyCollection(unusedArguments)
                    );
                }
            }

            assert !warningMessage.isBlank();
            processingContext.addMessages(warningMessage, ProcessingContext.MessageType.WARNING);
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
            // waiting for confirmation
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
