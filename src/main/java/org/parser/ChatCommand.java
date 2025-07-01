package org.parser;

import org.Helper;
import org.Modifier;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatCommand {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ChatCommand.class);

    public static final String ACTION_PREFIX = "!";
    public static final String MODIFIER_PREFIX = "-";

    private final String chatAction;
    private final Map<String, List<String>> chatModifiers;

    public ChatCommand(String content) {
        String[] tokens = content.split(" ");

        this.chatAction = tokens[0].substring(1).toUpperCase();
        this.chatModifiers = new HashMap<>();

        int tokenIndex = 1;
        while (tokenIndex < tokens.length) {
            String token = tokens[tokenIndex];

            if (!token.startsWith(ChatCommand.MODIFIER_PREFIX) || token.length() < 2) {
                ChatCommand.LOGGER.warn("Ignored token \"{}\"", token);
                ++tokenIndex;
                continue;
            }

            String modifier = token.substring(1);

            if (!modifier.equals("value")) {
                List<String> arguments = new ArrayList<>();

                int argumentIndex = tokenIndex + 1;
                while (argumentIndex < tokens.length) {
                    String argument = tokens[argumentIndex];
                    if (argument.startsWith(ChatCommand.MODIFIER_PREFIX)) {
                        break;
                    }

                    arguments.add(argument.toUpperCase());
                    ++argumentIndex;
                }

                arguments.removeIf(String::isBlank);
                this.chatModifiers.put(modifier.toUpperCase(), arguments);

                tokenIndex = argumentIndex;
            } else {
                StringBuilder stringBuilder = new StringBuilder();

                for (++tokenIndex; tokenIndex < tokens.length; ++tokenIndex) {
                    String argument = tokens[tokenIndex];
                    stringBuilder.append(argument);

                    if (argument.startsWith("-") && argument.length() > 1) {
                        ChatCommand.LOGGER.warn("Modifier \"{}\" possibly ignored, modifier \"value\" should be last",
                                argument.substring(1));
                    }

                    if (tokenIndex != tokens.length - 1) {
                        stringBuilder.append(' ');
                    }
                }

                this.chatModifiers.put(modifier.toUpperCase(), List.of(stringBuilder.toString()));
            }
        }
    }

    public String getChatAction() {
        return this.chatAction;
    }

    public Helper.TypedValue getArgument(Modifier<? extends Enum<?>> modifier) {
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

    public List<Helper.TypedValue> getArguments(Modifier<? extends Enum<?>> modifier) {
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
