package org.parser;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.interactions.commands.SlashCommandReference;
import org.Helper;
import org.Modifier;
import org.ProcessingContext;
import org.action.ActionHandler;
import org.exception.MissingArgumentException;
import org.javatuples.Quartet;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ChatCommand {
    private static final String ACTION_PREFIX = "!";

    private static final String MODIFIER_PREFIX = "-";
    private static final String QUOTE_DELIMIER = "\"";
    private static final String ESCAPE_EXPRESSION = "ESCAPE";
    private static final Set<String> VALID_ESCAPE_CHARACTERS = Set.of(
            "+", "%", "=", "^", "~", "\\", "/", "|", "*"
    );

    private ActionHandler action;
    private Map<Enum<?>, List<Helper.TypedValue>> modifiers;

    public ChatCommand(Message message, Map<String, ActionHandler> registeredActionHandlers, ProcessingContext processingContext) {
        if (!ChatCommand.isValidAction(message.getContentRaw(), registeredActionHandlers)) {
            return;
        }

        this.action = ChatCommand.parseAction(message.getContentRaw(), registeredActionHandlers);
        int escapeCharacterIndex = ChatCommand.parseEscapeCharacter(message.getContentRaw(), processingContext);
        ChatCommand.processMentions(message, processingContext);
        IndexedMessage indexedMessage = ChatCommand.parseMessage(message.getContentDisplay(), escapeCharacterIndex, processingContext);

        if (processingContext.hasParsingErrorMessage()) {
            return;
        }


        this.modifiers = new HashMap<>();

        Class<? extends Enum<?>> actionModifierEnum = this.action.getActionModifierEnum();
        Set<String> actionPossibleModifiers = Arrays.stream(actionModifierEnum.getEnumConstants()).map(Enum::toString).collect(Collectors.toSet());

        assert indexedMessage != null;
        Quartet<Integer, Integer, String, List<IndexedMessage.TypedCharacter>> indexedToken =
                indexedMessage.getTokenStartingFromIndex(0);

        while (indexedToken != null) {
            IndexedMessage.TypedCharacter firstC = indexedToken.getValue3().getFirst();
            if ((indexedToken.getValue2().equals("\"\"") && indexedToken.getValue3().getFirst().characterType() == IndexedMessage.TypedCharacter.CharacterType.SENTENCE_START)
                    || firstC.characterType() == IndexedMessage.TypedCharacter.CharacterType.LITERAL
                    || firstC.characterType() == IndexedMessage.TypedCharacter.CharacterType.SENTENCE_START) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored", indexedToken.getValue2()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                continue;
            }


            String strModifier = indexedToken.getValue2().substring(1).toUpperCase();
            if (strModifier.length() < ChatCommand.MODIFIER_PREFIX.length() + 1) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored", strModifier),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                continue;
            } else if (!actionPossibleModifiers.contains(strModifier)) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored because it is not a valid modifier for action \"{1}\"", indexedToken.getValue2(), this.action.toString()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                continue;
            }


            Enum<?> actionModifierEnumerator = this.action.getActionModifierEnumerator(strModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.action.getModifier(actionModifierEnumerator);
            if (this.modifiers.containsKey(actionModifierEnumerator)) {
                processingContext.addMessages(
                        MessageFormat.format("Modifier \"{0}\" was ignored because it is already present", actionModifierEnumerator.toString()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                continue;
            }


            List<String> arguments = new ArrayList<>();
            while (true) {
                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                if (indexedToken == null) {
                    break;
                } else if (indexedToken.getValue3().getFirst().characterType() == IndexedMessage.TypedCharacter.CharacterType.MODIFIER
                        && actionPossibleModifiers.contains(indexedToken.getValue2().substring(ChatCommand.MODIFIER_PREFIX.length()).toUpperCase())) {
                    break;
                }

                arguments.add(indexedToken.getValue2());
            }

            if (arguments.isEmpty()) {
                arguments.add("");
            }

            List<Helper.TypedValue> parsedArguments = new ArrayList<>();
            for (String argument : arguments) {
                parsedArguments.add(ChatCommand.parseArgument(argument, modifier));
            }
            this.modifiers.putIfAbsent(actionModifierEnumerator, parsedArguments);
        }


        for (String possibleModifier : actionPossibleModifiers) {
            Enum<?> actionModifierEnumerator = this.action.getActionModifierEnumerator(possibleModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.action.getModifier(actionModifierEnumerator);

            if (!modifier.isSwitchModifier() && !this.modifiers.containsKey(actionModifierEnumerator)) {
                this.modifiers.putIfAbsent(
                        actionModifierEnumerator,
                        List.of(new Helper.TypedValue(
                                modifier.getDefaultArgumentType(), Helper.TypedValue.Resolution.MODIFIER_MISSING, modifier.getDefaultArgument(), modifier.getPossibleArgumentsEnumClass()
                        ))
                );
            }
        }
    }

    private static boolean isValidAction(String content, Map<String, ActionHandler> registeredActionHandlers) {
        if (content.isBlank() || content.length() < ChatCommand.ACTION_PREFIX.length() + 1) {
            return false;
        }

        if (!content.startsWith(ChatCommand.ACTION_PREFIX)) {
            return false;
        }

        int splitIndex = content.indexOf(' ');
        String firstToken = (splitIndex == -1) ? content : content.substring(1, splitIndex);
        String firstTokenAction = firstToken.toUpperCase();

        return registeredActionHandlers.containsKey(firstTokenAction);
    }

    private static ActionHandler parseAction(String content, Map<String, ActionHandler> registeredActionHandlers) {
        int splitIndex = content.indexOf(' ');
        String firstToken = (splitIndex == -1) ? content : content.substring(1, splitIndex);
        String firstTokenAction = firstToken.toUpperCase();

        return registeredActionHandlers.get(firstTokenAction);
    }

    private static int parseEscapeCharacter(String content, ProcessingContext processingContext) {
        if (processingContext.hasParsingErrorMessage()) {
            return -1;
        }


        int firstSpaceIndex = content.indexOf(' ');
        int secondSpaceIndex = content.indexOf(' ', firstSpaceIndex + 1);
        if (secondSpaceIndex == -1) {
            return -1;
        }

        String secondToken = content.substring(firstSpaceIndex + 1, secondSpaceIndex);
        if (!secondToken.equalsIgnoreCase(ChatCommand.ESCAPE_EXPRESSION)) {
            return -1;
        }

        if (secondSpaceIndex >= content.length() - 1) {
            return -1;
        }

        String potentialEscapeCharacter = String.valueOf(content.charAt(secondSpaceIndex + 1));
        if (secondSpaceIndex <= content.length() - 3) {
            char followingCharacter = content.charAt(secondSpaceIndex + 2);
            if (followingCharacter != ' ') {
                processingContext.addMessages(
                        "The defined escape charatcer is not valid - it must be a single character",
                        ProcessingContext.MessageType.PARSING_ERROR
                );
                return -1;
            }
        }

        if (!ChatCommand.VALID_ESCAPE_CHARACTERS.contains(potentialEscapeCharacter)) {
            StringBuilder stringBuilder = new StringBuilder();
            ChatCommand.VALID_ESCAPE_CHARACTERS.forEach(element -> stringBuilder.append(element).append(' '));
            stringBuilder.setLength(stringBuilder.length() - 1);

            processingContext.addMessages(
                    MessageFormat.format("The defined escape character is not valid \"{0}\", valid characters are \"{1}\"", potentialEscapeCharacter, stringBuilder.toString()),
                    ProcessingContext.MessageType.PARSING_ERROR
            );
            return -1;
        }

        return secondSpaceIndex + 1;
    }

    private static void processMentions(Message message, ProcessingContext processingContext) {
        if (processingContext.hasParsingErrorMessage()) {
            return;
        }


        Mentions mentions = message.getMentions();

        List<User> users = mentions.getUsers();
        List<Member> members = mentions.getMembers();

        List<GuildChannel> channels = mentions.getChannels();
        List<Role> roles = mentions.getRoles();
        List<CustomEmoji> emojis = mentions.getCustomEmojis();
        List<SlashCommandReference> slashCommands = mentions.getSlashCommands();

        if (!slashCommands.isEmpty()) {
            processingContext.addMessages("Slash commands are not allowed in command messages", ProcessingContext.MessageType.PARSING_ERROR);
            return;
        }

        if (users.isEmpty() && members.isEmpty() && channels.isEmpty() && roles.isEmpty() && emojis.isEmpty()) {
            return;
        }


        if (members.isEmpty()) {
            for (User user : users) {
                processingContext.addMessages(
                        MessageFormat.format(
                                "Detected user mention in message, raw content \"{0}\" replaced with \"{1}\"",
                                "<@" + user.getId() + ">",
                                "@" + user.getGlobalName()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );
            }
        } else {
            for (Member member : members) {
                processingContext.addMessages(
                        MessageFormat.format(
                                "Detected member mention in message, raw content \"{0}\" replaced with \"{1}\"",
                                "<@" + member.getId() + ">",
                                "@" + member.getNickname()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );
            }
        }

        for (GuildChannel channel : channels) {
            processingContext.addMessages(
                    MessageFormat.format(
                            "Detected channel mention in message, raw content \"{0}\" replaced with \"{1}\"",
                            "<#" + channel.getId() + ">",
                            "#" + channel.getName()),
                    ProcessingContext.MessageType.PARSING_WARNING
            );
        }

        for (Role role : roles) {
            processingContext.addMessages(
                    MessageFormat.format(
                            "Detected role mention in message, raw content \"{0}\" replaced with \"{1}\"",
                            "<@&" + role.getId() + ">",
                            "@" + role.getName()),
                    ProcessingContext.MessageType.PARSING_WARNING
            );
        }

        for (CustomEmoji emoji : emojis) {
            processingContext.addMessages(
                    MessageFormat.format(
                            "Detected custom emoji in message, raw content \"{0}\" replaced with \"{1}\"",
                            "<:" + emoji.getName() + ":" + emoji.getId() + ">",
                            ":" + emoji.getName() + ":"),
                    ProcessingContext.MessageType.PARSING_WARNING
            );
        }
    }

    private static IndexedMessage parseMessage(String content, int escapeCharacterIndex, ProcessingContext processingContext) {
        if (processingContext.hasParsingErrorMessage()) {
            return null;
        }


        IndexedMessage indexedMessage = new IndexedMessage();

        int i = (escapeCharacterIndex != -1) ? escapeCharacterIndex + 2 : content.indexOf(' ') + 1;
        Character escapeCharacter = (escapeCharacterIndex == -1) ? null : content.charAt(escapeCharacterIndex);

        boolean escapeNext = false;
        boolean inSentence = false;

        for (; i < content.length(); ++i) {
            char c = content.charAt(i);
            if (c == ' ') {
                if (inSentence) {
                    indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                } else if (i - 1 > 0) {
                    char previous = content.charAt(i - 1);
                    if (previous != ' ') {
                        indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                    }
                }

                continue;
            }

            if (escapeNext) {
                escapeNext = false;

                ChatCommand.warnIfEscapeWasUseless(c, escapeCharacter, processingContext);
                indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                continue;
            }

            if (escapeCharacter != null && escapeCharacter.equals(c)) {
                escapeNext = true;
                continue;
            }

            if (ChatCommand.MODIFIER_PREFIX.equals(String.valueOf(c))) {
                char previous = content.charAt(i - 1);
                if (previous == ' ' && !inSentence) {
                    indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.MODIFIER);
                } else {
                    indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                }

                continue;
            }

            if (ChatCommand.QUOTE_DELIMIER.equals(String.valueOf(c))) {
                if (inSentence) {
                    if (i + 1 < content.length()) {
                        char next = content.charAt(i + 1);
                        if (next == ' ') {
                            inSentence = false;
                            indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.SENTENCE_END);
                        } else {
                            indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                        }
                    } else {
                        inSentence = false;
                        indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.SENTENCE_END);
                    }
                } else {
                    char previous = content.charAt(i - 1);
                    if (previous == ' ') {
                        inSentence = true;
                        indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.SENTENCE_START);
                    } else {
                        indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                    }
                }

                continue;
            }

            indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
        }


        if (inSentence) {
            processingContext.addMessages("Unclosed quoted string detected", ProcessingContext.MessageType.PARSING_ERROR);
            return null;
        }

        if (escapeNext) {
            processingContext.addMessages("EOF should not be escaped", ProcessingContext.MessageType.PARSING_WARNING);
        }

        return indexedMessage;
    }

    private static void warnIfEscapeWasUseless(char escapedCharacter, char escapeCharacter, ProcessingContext processingContext) {
        String strEscapedCharacter = String.valueOf(escapedCharacter);
        if (!strEscapedCharacter.equals(ChatCommand.MODIFIER_PREFIX) && !strEscapedCharacter.equals(ChatCommand.QUOTE_DELIMIER)
                && !strEscapedCharacter.equals(String.valueOf(escapeCharacter))) {
            processingContext.addMessages(
                    MessageFormat.format("Character \"{0}\" should not be escaped", escapedCharacter),
                    ProcessingContext.MessageType.PARSING_WARNING
            );
        }
    }

    public ActionHandler getAction() {
        return this.action;
    }

    public<T extends Enum<T>> boolean isSwitchModifierPresent(T modifier, ProcessingContext processingContext) {
        List<Helper.TypedValue> arguments = this.modifiers.get(modifier);
        if (arguments == null || arguments.isEmpty()) {
            return false;
        }

        if (arguments.stream().anyMatch(element -> element.type() != Helper.TypedValue.Type.SWITCH)) {
            throw new IllegalStateException("Modifier is not a switch modifier");
        }

        if (arguments.stream().anyMatch(element -> element.resolution() != Helper.TypedValue.Resolution.ARGUMENT_MISSING)) {
            processingContext.addMessages(
                    MessageFormat.format("Modifier \"{0}\" is a switch modifier, it should not be provided with any arguments", modifier),
                    ProcessingContext.MessageType.WARNING
            );
        }

        return true;
    }

    public<T extends Enum<T>, U extends Enum<U>> U getFirstArgumentAsEnum(T modifier, Class<U> requiredEnumClass, ProcessingContext processingContext) {
        List<Helper.TypedValue> arguments = this.modifiers.get(modifier);
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
        List<Helper.TypedValue> arguments = this.modifiers.get(modifier);
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
        List<Helper.TypedValue> arguments = this.modifiers.get(modifier);
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
