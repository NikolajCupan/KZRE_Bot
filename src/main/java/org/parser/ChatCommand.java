package org.parser;

import org.Helper;
import org.Modifier;
import org.ProcessingContext;
import org.action.ActionHandler;
import org.exception.MissingArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ChatCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatCommand.class);

    public static final String ACTION_PREFIX = "!";
    public static final String MODIFIER_PREFIX = "-";

    private ActionHandler actionFromChat = null;
    private Map<Enum<?>, List<Helper.TypedValue>> modifiersFromChat = null;

    public ChatCommand(String content, Map<String, ActionHandler> registeredActionHandlers) {
        String[] tokens = content.split(" ");
        if (tokens.length < 1) {
            return;
        }

        String firstToken = tokens[0].toUpperCase();
        if (!firstToken.startsWith(ChatCommand.ACTION_PREFIX)) {
            return;
        }

        String firstTokenAction = firstToken.substring(ChatCommand.ACTION_PREFIX.length());
        if (!registeredActionHandlers.containsKey(firstTokenAction)) {
            return;
        }


        this.actionFromChat = registeredActionHandlers.get(firstTokenAction);
        this.modifiersFromChat = new HashMap<>();

        Class<? extends Enum<?>> actionModifierEnum = this.actionFromChat.getActionModifierEnum();
        Set<String> actionPossibleModifiers = Arrays.stream(actionModifierEnum.getEnumConstants()).map(Enum::toString).collect(Collectors.toSet());


        int tokenIndex = 1;
        while (tokenIndex < tokens.length) {
            String token = tokens[tokenIndex].toUpperCase();

            if (!token.startsWith(ChatCommand.MODIFIER_PREFIX) || token.length() < ChatCommand.MODIFIER_PREFIX.length() + 1) {
                ChatCommand.LOGGER.warn("Token \"{}\" was ignored", token);
                ++tokenIndex;
                continue;
            }

            String strModifier = token.substring(ChatCommand.MODIFIER_PREFIX.length());
            if (!actionPossibleModifiers.contains(strModifier)) {
                ChatCommand.LOGGER.warn("Token \"{}\" was ignored because it is not a valid modifier for action \"{}\"", token, this.actionFromChat.toString());
                ++tokenIndex;
                continue;
            }


            Enum<?> actionModifierEnumerator = this.actionFromChat.getActionModifierEnumerator(strModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.actionFromChat.getModifier(actionModifierEnumerator);
            if (this.modifiersFromChat.containsKey(actionModifierEnumerator)) {
                ChatCommand.LOGGER.warn("Modifier \"{}\" was ignored because it is already present", actionModifierEnumerator.toString());
                ++tokenIndex;
                continue;
            }


            if (!modifier.isConsumeRest()) {
                Set<String> arguments = new LinkedHashSet<>();

                int argumentIndex = tokenIndex + 1;
                while (argumentIndex < tokens.length) {
                    String argument = tokens[argumentIndex].toUpperCase();
                    if (argument.startsWith(ChatCommand.MODIFIER_PREFIX)
                            && actionPossibleModifiers.contains(argument.substring(ChatCommand.MODIFIER_PREFIX.length()))) {
                        break;
                    }

                    arguments.add(argument);
                    ++argumentIndex;
                }

                arguments.removeIf(String::isBlank);
                if (arguments.isEmpty()) {
                    arguments.add("");
                }

                List<Helper.TypedValue> parsedArguments = new ArrayList<>();
                for (String argument : arguments) {
                    parsedArguments.add(ChatCommand.parseArgument(argument, modifier));
                }
                this.modifiersFromChat.putIfAbsent(actionModifierEnumerator, parsedArguments);

                tokenIndex = argumentIndex;
            } else {
                StringBuilder stringBuilder = new StringBuilder();

                for (++tokenIndex; tokenIndex < tokens.length; ++tokenIndex) {
                    String argument = tokens[tokenIndex].toUpperCase();
                    stringBuilder.append(argument);

                    if (tokenIndex != tokens.length - 1) {
                        stringBuilder.append(' ');
                    }
                }

                this.modifiersFromChat.putIfAbsent(actionModifierEnumerator, List.of(ChatCommand.parseArgument(stringBuilder.toString(), modifier)));
            }
        }

        for (String possibleModifier : actionPossibleModifiers) {
            Enum<?> actionModifierEnumerator = this.actionFromChat.getActionModifierEnumerator(possibleModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.actionFromChat.getModifier(actionModifierEnumerator);

            if (!this.modifiersFromChat.containsKey(actionModifierEnumerator)) {
                this.modifiersFromChat.putIfAbsent(
                        actionModifierEnumerator,
                        List.of(new Helper.TypedValue(
                            modifier.getDefaultArgumentType(), Helper.TypedValue.Resolution.MODIFIER_MISSING, modifier.getDefaultArgument(), modifier.getPossibleArgumentsEnumClass()
                        ))
                );
            }
        }
    }

    public ActionHandler getAction() {
        return this.actionFromChat;
    }

    public<T extends Enum<T>, U extends Enum<U>> U getFirstArgumentAsEnum(T modifier, Class<U> requiredEnumClass, ProcessingContext processingContext) {
        List<Helper.TypedValue> arguments = this.modifiersFromChat.get(modifier);
        Helper.TypedValue firstArgument = arguments.getFirst();

        ChatCommand.addWarningIfMultipleArgumentsArePresent(modifier, arguments, processingContext);
        ChatCommand.addWarningIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (firstArgument.type() == Helper.TypedValue.Type.NULL) {
            throw new MissingArgumentException(firstArgument.getStateMessage(modifier.toString(), false));
        } else if (firstArgument.type() != Helper.TypedValue.Type.ENUMERATOR) {
            throw new IllegalStateException("Argument is not enumerator");
        }

        Class<? extends Enum<?>> actualType = firstArgument.enumClass();
        if (actualType != requiredEnumClass) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Actual enum type \"{0}\" and required enum type \"{1}\" are different", actualType, requiredEnumClass)
            );
        }

        return requiredEnumClass.cast(Enum.valueOf(requiredEnumClass, firstArgument.value()));
    }

    public<T extends Enum<T>> Helper.TypedValue getFirstArgument(T modifier, boolean allowNullArgument, ProcessingContext processingContext) {
        List<Helper.TypedValue> arguments = this.modifiersFromChat.get(modifier);
        Helper.TypedValue firstArgument = arguments.getFirst();

        ChatCommand.addWarningIfMultipleArgumentsArePresent(modifier, arguments, processingContext);
        ChatCommand.addWarningIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (!allowNullArgument) {
            if (firstArgument.type() == Helper.TypedValue.Type.NULL) {
                throw new MissingArgumentException(firstArgument.getStateMessage(modifier.toString(), false));
            }
        }


        return firstArgument;
    }

    public<T extends Enum<T>> Helper.TypedValue getFirstArgumentFirstWord(T modifier, boolean allowNullArgument, ProcessingContext processingContext) {
        List<Helper.TypedValue> arguments = this.modifiersFromChat.get(modifier);
        Helper.TypedValue firstArgument = arguments.getFirst();

        ChatCommand.addWarningIfMultipleArgumentsArePresent(modifier, arguments, processingContext);
        ChatCommand.addWarningIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (!allowNullArgument) {
            if (firstArgument.type() == Helper.TypedValue.Type.NULL) {
                throw new MissingArgumentException(firstArgument.getStateMessage(modifier.toString(), false));
            }
        }

        String firstArgumentFirstWord = firstArgument.valueFirstWord();
        if (!firstArgumentFirstWord.equals(firstArgument.value())) {
            processingContext.addMessages(
                    MessageFormat.format("Argument for modifier \"{0}\" should not contain spaces, everything after first word was ignored", modifier.toString()),
                    ProcessingContext.MessageType.WARNING
            );
        }

        return new Helper.TypedValue(
            firstArgument.type(), firstArgument.resolution(), firstArgument.valueFirstWord(), firstArgument.enumClass()
        );
    }

    private static<T extends Enum<T>> void addWarningIfMultipleArgumentsArePresent(T modifier, List<Helper.TypedValue> arguments, ProcessingContext processingContext) {
        if (arguments.size() > 1) {
            processingContext.addMessages(
                    MessageFormat.format("Multiple arguments for modifier \"{0}\" found, everything after first argument was ignored", modifier),
                    ProcessingContext.MessageType.WARNING
            );
        }
    }

    private static<T extends Enum<T>> void addWarningIfResolutionIsNotValidArgument(T modifier, Helper.TypedValue argument, ProcessingContext processingContext) {
        if (argument.resolution() != Helper.TypedValue.Resolution.ARGUMENT_VALID) {
            processingContext.addMessages(
                    argument.getStateMessage(modifier.toString(), true),
                    ProcessingContext.MessageType.WARNING
            );
        }
    }

    private static Helper.TypedValue parseArgument(String argument, Modifier<? extends Enum<?>, ? extends Number> modifier) {
        if (argument.isBlank()) {
            return new Helper.TypedValue(
                    modifier.getDefaultArgumentType(), Helper.TypedValue.Resolution.ARGUMENT_MISSING, modifier.getDefaultArgument(), modifier.getPossibleArgumentsEnumClass()
            );
        }

        if (!modifier.isPossibleArgument(argument)) {
            return new Helper.TypedValue(
                    modifier.getDefaultArgumentType(), Helper.TypedValue.Resolution.ARGUMENT_INVALID, modifier.getDefaultArgument(), modifier.getPossibleArgumentsEnumClass()
            );
        }

        return new Helper.TypedValue(
                modifier.getChatArgumentType(argument), Helper.TypedValue.Resolution.ARGUMENT_VALID, argument, modifier.getPossibleArgumentsEnumClass()
        );
    }
}
