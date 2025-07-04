package org.parser;

import org.Helper;
import org.MessagesListener;
import org.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChatCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatCommand.class);

    private static final Set<String> ACTIONS_MODIFIERS_KEYWORDS = MessagesListener.getActionsModifiersKeywords();
    static {
        ChatCommand.ACTIONS_MODIFIERS_KEYWORDS.add("VALUE");
    }

    public static final String ACTION_PREFIX = "!";
    public static final String MODIFIER_PREFIX = "-";

    private final String chatAction;
    private final Map<String, List<String>> chatModifiers;

    public ChatCommand(String content) {
        String[] tokens = content.split(" ");

        this.chatAction = tokens[0].substring(ChatCommand.ACTION_PREFIX.length()).toUpperCase();
        this.chatModifiers = new HashMap<>();

        int tokenIndex = 1;
        while (tokenIndex < tokens.length) {
            String token = tokens[tokenIndex].toUpperCase();

            if (!token.startsWith(ChatCommand.MODIFIER_PREFIX) || token.length() < ChatCommand.MODIFIER_PREFIX.length() + 1) {
                ChatCommand.LOGGER.warn("Ignored token \"{}\"", token);
                ++tokenIndex;
                continue;
            }

            String modifier = token.substring(ChatCommand.MODIFIER_PREFIX.length());

            if (!modifier.equalsIgnoreCase("VALUE")) {
                List<String> arguments = new ArrayList<>();

                int argumentIndex = tokenIndex + 1;
                while (argumentIndex < tokens.length) {
                    String argument = tokens[argumentIndex].toUpperCase();
                    if (argument.startsWith(ChatCommand.MODIFIER_PREFIX)
                            && ChatCommand.ACTIONS_MODIFIERS_KEYWORDS.contains(argument.substring(ChatCommand.MODIFIER_PREFIX.length()))) {
                        break;
                    }

                    arguments.add(argument);
                    ++argumentIndex;
                }

                arguments.removeIf(String::isBlank);
                this.chatModifiers.put(modifier, arguments);

                tokenIndex = argumentIndex;
            } else {
                StringBuilder stringBuilder = new StringBuilder();

                for (++tokenIndex; tokenIndex < tokens.length; ++tokenIndex) {
                    String argument = tokens[tokenIndex].toUpperCase();
                    stringBuilder.append(argument);

                    if (argument.startsWith(ChatCommand.MODIFIER_PREFIX)
                            && argument.length() > ChatCommand.MODIFIER_PREFIX.length()) {
                        ChatCommand.LOGGER.warn("Modifier \"{}\" possibly ignored, modifier \"VALUE\" should be last",
                                argument.substring(ChatCommand.MODIFIER_PREFIX.length()));
                    }

                    if (tokenIndex != tokens.length - 1) {
                        stringBuilder.append(' ');
                    }
                }

                this.chatModifiers.put(modifier, List.of(stringBuilder.toString()));
            }
        }
    }

    public String getChatAction() {
        return this.chatAction;
    }

    public<T extends Enum<T>, U extends Enum<U>, V extends Enum<V>> V getArgumentAsEnum(Modifier<T, U> modifier, Class<V> requiredType) {
        Helper.TypedValue argument = this.getArgument(modifier);
        if (argument.type() != Helper.TypedValue.Type.ENUMERATOR) {
            throw new RuntimeException("Argument is not enumerator");
        }

        return requiredType.cast(Enum.valueOf(requiredType, argument.value()));
    }

    public<T extends Enum<T>, U extends Enum<U>> Helper.TypedValue getArgument(Modifier<T, U> modifier) {
        if (!this.chatModifiers.containsKey(modifier.getModifier().toString())) {
            return modifier.getDefaultArgument();
        }

        List<String> chatArguments = this.chatModifiers.get(modifier.getModifier().toString());
        if (chatArguments.isEmpty()) {
            return modifier.getDefaultArgument();
        }

        String firstChatArgument = chatArguments.getFirst();
        if (!modifier.isPossibleArgument(firstChatArgument)) {
            return modifier.getDefaultArgument();
        }

        return new Helper.TypedValue(modifier.getChatArgumentType(firstChatArgument), firstChatArgument);
    }

    public<T extends Enum<T>, U extends Enum<U>> List<Helper.TypedValue> getArguments(Modifier<T, U> modifier) {
        if (!this.chatModifiers.containsKey(modifier.getModifier().toString())) {
            return List.of(modifier.getDefaultArgument());
        }

        List<String> chatArguments = this.chatModifiers.get(modifier.getModifier().toString());
        if (chatArguments.isEmpty()) {
            return List.of(modifier.getDefaultArgument());
        }

        return chatArguments.stream()
                .map(element -> new Helper.TypedValue(modifier.getChatArgumentType(element), element))
                .collect(Collectors.toList());
    }
}
