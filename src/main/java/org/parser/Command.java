package org.parser;

import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Command {
    private record Modifier(String name, List<String> arguments) {
    }

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Command.class);

    public static final String ACTION_PREFIX = "!";
    public static final String MODIFIER_PREFIX = "-";

    private final String action;
    private final List<Modifier> modifiers;

    public Command(String content) {
        String[] tokens = content.split(" ");

        this.action = tokens[0].substring(1);
        this.modifiers = new ArrayList<>();

        int tokenIndex = 1;
        while (tokenIndex < tokens.length) {
            String token = tokens[tokenIndex];

            if (!token.startsWith(Command.MODIFIER_PREFIX) || token.length() < 2) {
                Command.LOGGER.warn("Ignored token \"{}\"", token);
                ++tokenIndex;
                continue;
            }

            String modifier = token.substring(1);

            if (!modifier.equals("value")) {
                List<String> arguments = new ArrayList<>();

                int argumentIndex = tokenIndex + 1;
                while (argumentIndex < tokens.length) {
                    String argument = tokens[argumentIndex];
                    if (argument.startsWith(Command.MODIFIER_PREFIX)) {
                        break;
                    }

                    arguments.add(argument);
                    ++argumentIndex;
                }

                arguments.removeIf(String::isBlank);
                this.modifiers.add(new Modifier(modifier, arguments));

                tokenIndex = argumentIndex;
            } else {
                StringBuilder stringBuilder = new StringBuilder();

                for (++tokenIndex; tokenIndex < tokens.length; ++tokenIndex) {
                    String argument = tokens[tokenIndex];
                    stringBuilder.append(argument);

                    if (argument.startsWith("-") && argument.length() > 1) {
                        Command.LOGGER.warn("Modifier \"{}\" possibly ignored, modifier \"value\" should be last", argument.substring(1));
                    }

                    if (tokenIndex != tokens.length - 1) {
                        stringBuilder.append(' ');
                    }
                }

                this.modifiers.add(new Modifier(modifier, List.of(stringBuilder.toString())));
            }
        }
    }

    public String getAction() {
        return this.action;
    }
}
