package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.Action;
import org.action.ActionHandler;
import org.action.Quote;
import org.database.GuildManager;
import org.database.UserManager;
import org.jetbrains.annotations.NotNull;
import org.parsing.ChatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.Constants;
import org.utility.ProcessingContext;
import org.utility.TypedValue;

import java.awt.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;

public class MessageListener extends ListenerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);

    private static final Map<String, ActionHandler> REGISTERED_ACTION_HANDLERS = new HashMap<>();
    private static final UserManager USER_MANAGER = new UserManager();
    private static final GuildManager GUILD_MANAGER = new GuildManager();

    static {
        MessageListener.REGISTERED_ACTION_HANDLERS.put(Action.QUOTE.toString().toUpperCase(), new Quote());
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
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (Main.COMMAND_LINE_ARGUMENTS.contains(Constants.DEVELOPMENT_ARGUMENT)
                && !event.getGuild().getId().equals("949756585616474152")) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }


        ProcessingContext processingContext = new ProcessingContext();
        ChatCommand chatCommand = new ChatCommand(event.getMessage(), MessageListener.REGISTERED_ACTION_HANDLERS, processingContext);
        ActionHandler actionHandler = chatCommand.getActionHandler();
        if (actionHandler == null) {
            return;
        }


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);

        if (processingContext.hasParsingErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        } else {
            MessageListener.LOGGER.info("Received action \"{}\"", event.getMessage().getContentRaw());

            MessageListener.USER_MANAGER.refreshUser(event);
            MessageListener.GUILD_MANAGER.refreshGuild(event);

            actionHandler.executeAction(event, chatCommand, processingContext);
            MessageListener.warnIfUnusedModifiersOrArgumentsExist(chatCommand, processingContext);

            if (processingContext.hasErrorMessage()) {
                processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                        embedBuilder.addField(element.messageType().toString(), element.message(), false)
                );
            } else {
                processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT)).forEach(element ->
                        embedBuilder.addField(element.messageType().toString(), element.message(), false)
                );
            }
        }

        if (!processingContext.hasParsingErrorMessage() && chatCommand.isSwitchModifierPresent(ActionHandler.GlobalActionModifier.VERBOSE)) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_WARNING, ProcessingContext.MessageType.WARNING)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }


        if (!embedBuilder.isEmpty()) {
            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }
}
