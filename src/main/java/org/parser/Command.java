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
            if (!token.startsWith(Command.MODIFIER_PREFIX)) {
                Command.LOGGER.warn("Ignored token \"{}\"", token);
                ++tokenIndex;
            }

            String modifier = token.substring(1);
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
            this.modifiers.add(new Modifier(modifier, arguments));

            tokenIndex = argumentIndex;
        }
    }

    public String getAction() {
        return this.action;
    }
}
