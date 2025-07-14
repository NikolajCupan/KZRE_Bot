package org.parsing;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.interactions.commands.SlashCommandReference;
import org.utility.ProcessingContext;
import org.utility.TypedValue;
import org.action.ActionHandler;
import org.exception.MissingArgumentException;
import org.javatuples.Quartet;

import java.text.MessageFormat;
import java.util.*;

public class ChatCommand {
    private static final String ACTION_PREFIX = "!";

    private static final String MODIFIER_PREFIX = "-";
    private static final String QUOTE_DELIMIER = "\"";
    private static final String ESCAPE_EXPRESSION = "ESCAPE";
    private static final Set<String> VALID_ESCAPE_CHARACTERS = Set.of(
            "+", "%", "=", "^", "~", "\\", "/", "|", "*"
    );

    private ActionHandler action;
    private ModifierMap modifierMap;

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


        this.modifierMap = new ModifierMap();
        Set<String> actionPossibleModifiers = this.action.getActionPossibleModifiers();

        assert indexedMessage != null;
        Quartet<Integer, Integer, String, List<IndexedMessage.TypedCharacter>> indexedToken =
                indexedMessage.getTokenStartingFromIndex(0);

        while (indexedToken != null) {
            if (indexedToken.getValue2().isEmpty()) {
                processingContext.addMessages(
                        "Empty token [\"\"] detected, it was ignored",
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.getValue1());
                continue;
            }

            IndexedMessage.TypedCharacter firstC = indexedToken.getValue3().getFirst();
            if (firstC.characterType() == IndexedMessage.TypedCharacter.CharacterType.LITERAL
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
            if (this.modifierMap.containsKey(actionModifierEnumerator)) {
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
                } else if (indexedToken.getValue2().isEmpty()) {
                    processingContext.addMessages(
                            "Empty token [\"\"] detected, it was ignored",
                            ProcessingContext.MessageType.PARSING_WARNING
                    );
                } else {
                    arguments.add(indexedToken.getValue2());
                }
            }

            if (!modifier.getIsSwitchModifier() && arguments.isEmpty()) {
                arguments.add("");
            }

            List<TypedValue> parsedArguments = new ArrayList<>();
            for (String argument : arguments) {
                parsedArguments.add(ChatCommand.parseArgument(argument, modifier));
            }
            this.modifierMap.putIfAbsent(actionModifierEnumerator, parsedArguments, false);
        }


        for (String possibleModifier : actionPossibleModifiers) {
            Enum<?> actionModifierEnumerator = this.action.getActionModifierEnumerator(possibleModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.action.getModifier(actionModifierEnumerator);

            if (!modifier.getIsSwitchModifier() && !this.modifierMap.containsKey(actionModifierEnumerator)) {
                this.modifierMap.putIfAbsent(
                        actionModifierEnumerator,
                        List.of(new TypedValue(
                                modifier.getDefaultArgumentType(), TypedValue.Resolution.MODIFIER_MISSING, modifier.getDefaultArgument(), "", modifier.getPossibleArgumentsEnumClass()
                        )), true
                );
            }
        }


        StringBuilder stringBuilder = new StringBuilder();
        indexedMessage.getTypedCharacters().forEach(element -> {
            if (element.characterType() == IndexedMessage.TypedCharacter.CharacterType.LITERAL) {
                stringBuilder.append(element.character());
            } else {
                stringBuilder.append("`").append(element.character()).append("`");
            }
        });

        processingContext.addMessages(
                stringBuilder.toString(),
                ProcessingContext.MessageType.DEBUG
        );
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
                        "The defined escape character is not valid - it must be a single character",
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

    private static TypedValue parseArgument(String argument, Modifier<? extends Enum<?>, ? extends Number> modifier) {
        if (argument.isBlank()) {
            return new TypedValue(
                    modifier.getDefaultArgumentType(), TypedValue.Resolution.ARGUMENT_MISSING, modifier.getDefaultArgument(), argument, modifier.getPossibleArgumentsEnumClass()
            );
        }

        if (!modifier.isPossibleArgument(argument)) {
            return new TypedValue(
                    modifier.getDefaultArgumentType(), TypedValue.Resolution.ARGUMENT_INVALID, modifier.getDefaultArgument(), argument, modifier.getPossibleArgumentsEnumClass()
            );
        }

        return new TypedValue(
                modifier.getChatArgumentType(argument), TypedValue.Resolution.ARGUMENT_VALID, argument, argument, modifier.getPossibleArgumentsEnumClass()
        );
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

    private static<T extends Enum<T>> void warnIfResolutionIsNotValidArgument(T modifier, TypedValue argument, ProcessingContext processingContext) {
        if (argument.getResolution() != TypedValue.Resolution.ARGUMENT_VALID) {
            processingContext.addMessages(
                    argument.getStateMessage(modifier.toString(), true),
                    ProcessingContext.MessageType.WARNING
            );
        }
    }

    public<T extends Enum<T>> TypedValue getFirstArgument(T modifier, boolean allowNullArgument, boolean setUsed, ProcessingContext processingContext) {
        List<TypedValue> arguments = this.modifierMap.get(modifier);
        TypedValue firstArgument = arguments.getFirst();

        ChatCommand.warnIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (!allowNullArgument) {
            if (firstArgument.getType() == TypedValue.Type.NULL) {
                throw new MissingArgumentException(firstArgument.getStateMessage(modifier.toString(), false));
            }
        }

        if (setUsed) {
            firstArgument.setUsed();
        }

        return firstArgument;
    }

    public<T extends Enum<T>, U extends Enum<U>> U getFirstArgumentAsEnum(T modifier, Class<U> requiredEnumClass, boolean setUsed, ProcessingContext processingContext) {
        List<TypedValue> arguments = this.modifierMap.get(modifier);
        TypedValue firstArgument = arguments.getFirst();

        ChatCommand.warnIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (firstArgument.getType() == TypedValue.Type.NULL) {
            throw new MissingArgumentException(firstArgument.getStateMessage(modifier.toString(), false));
        } else if (firstArgument.getType() != TypedValue.Type.ENUMERATOR) {
            throw new IllegalStateException("Argument is not enumerator");
        }

        Class<? extends Enum<?>> actualType = firstArgument.getEnumClass();
        if (actualType != requiredEnumClass) {
            throw new IllegalArgumentException(
                    MessageFormat.format("Actual enum type \"{0}\" and required enum type \"{1}\" are different", actualType, requiredEnumClass)
            );
        }

        if (setUsed) {
            firstArgument.setUsed();
        }

        return requiredEnumClass.cast(Enum.valueOf(requiredEnumClass, firstArgument.getUsedValue().toUpperCase()));
    }

    public<T extends Enum<T>> boolean isSwitchModifierPresent(T modifier) {
        if (!this.modifierMap.containsKey(modifier)) {
            return false;
        }

        List<TypedValue> arguments = this.modifierMap.get(modifier);
        if (arguments != null && !arguments.isEmpty()) {
            if (arguments.stream().anyMatch(element -> element.getType() != TypedValue.Type.SWITCH)) {
                throw new IllegalStateException("Modifier is not a switch modifier");
            }
        }

        return true;
    }

    public ActionHandler getAction() {
        return this.action;
    }

    public ModifierMap getModifierMap() {
        return this.modifierMap;
    }
}
