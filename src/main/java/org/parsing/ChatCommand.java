package org.parsing;

import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.interactions.commands.SlashCommandReference;
import org.utility.Helper;
import org.utility.ProcessingContext;
import org.utility.TypedValue;
import org.action.ActionHandler;
import org.exception.MissingArgumentException;

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

    private ActionHandler actionHandler;
    private ModifierMap modifierMap;

    public ChatCommand(Message message, Map<String, ActionHandler> registeredActionHandlers, ProcessingContext processingContext) {
        if (!ChatCommand.isValidAction(message.getContentRaw(), registeredActionHandlers)) {
            return;
        }

        this.actionHandler = ChatCommand.parseAction(message.getContentRaw(), registeredActionHandlers);
        int escapeCharacterIndex = ChatCommand.parseEscapeCharacter(message.getContentRaw(), processingContext);
        ChatCommand.processMentions(message, processingContext);
        IndexedMessage indexedMessage = ChatCommand.parseMessage(message.getContentDisplay(), escapeCharacterIndex, processingContext);

        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            return;
        }


        this.modifierMap = new ModifierMap();
        Set<String> actionPossibleModifiers = this.actionHandler.getPossibleModifiers();

        assert indexedMessage != null;
        IndexedMessage.Token indexedToken = indexedMessage.getTokenStartingFromIndex(0);

        while (indexedToken != null) {
            if (indexedToken.token().isEmpty()) {
                processingContext.addMessages(
                        "Empty token [\"\"] detected, it was ignored",
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());
                continue;
            }

            IndexedMessage.TypedCharacter firstC = indexedToken.typedCharacters().getFirst();
            if (firstC.characterType() == IndexedMessage.TypedCharacter.CharacterType.LITERAL
                    || firstC.characterType() == IndexedMessage.TypedCharacter.CharacterType.SENTENCE_START) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored", indexedToken.token()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());
                continue;
            }


            String strModifier = indexedToken.token().substring(ChatCommand.MODIFIER_PREFIX.length()).toUpperCase();
            if (strModifier.length() < ChatCommand.MODIFIER_PREFIX.length() + 1) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored", strModifier),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());
                continue;
            } else if (!actionPossibleModifiers.contains(strModifier)) {
                processingContext.addMessages(
                        MessageFormat.format("Token \"{0}\" was ignored because it is not a valid modifier for action \"{1}\"", indexedToken.token(), this.actionHandler.toString()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());
                continue;
            }


            Enum<?> actionModifierEnumerator = this.actionHandler.getModifierEnumerator(strModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.actionHandler.getModifier(actionModifierEnumerator);
            if (this.modifierMap.containsKey(actionModifierEnumerator)) {
                processingContext.addMessages(
                        MessageFormat.format("Modifier \"{0}\" was ignored because it is already present", actionModifierEnumerator.toString()),
                        ProcessingContext.MessageType.PARSING_WARNING
                );

                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());
                continue;
            }


            List<String> arguments = new ArrayList<>();
            while (true) {
                indexedToken = indexedMessage.getTokenStartingFromIndex(indexedToken.endIndex());

                if (indexedToken == null) {
                    break;
                } else if (indexedToken.typedCharacters().getFirst().characterType() == IndexedMessage.TypedCharacter.CharacterType.MODIFIER
                        && actionPossibleModifiers.contains(indexedToken.token().substring(ChatCommand.MODIFIER_PREFIX.length()).toUpperCase())) {
                    break;
                } else if (indexedToken.token().isEmpty()) {
                    processingContext.addMessages(
                            "Empty token [\"\"] detected, it was ignored",
                            ProcessingContext.MessageType.PARSING_WARNING
                    );
                } else {
                    arguments.add(indexedToken.token());
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
            Enum<?> actionModifierEnumerator = this.actionHandler.getModifierEnumerator(possibleModifier);
            Modifier<? extends Enum<?>, ? extends Number> modifier = this.actionHandler.getModifier(actionModifierEnumerator);

            if (!modifier.getIsSwitchModifier() && !this.modifierMap.containsKey(actionModifierEnumerator)) {
                this.modifierMap.putIfAbsent(
                        actionModifierEnumerator,
                        List.of(new TypedValue(
                                modifier.getDefaultArgumentType(), TypedValue.Resolution.MODIFIER_MISSING, modifier.getDefaultArgument(), "", modifier.getPossibleArgumentsEnumClass()
                        )), true
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
        String firstToken = (splitIndex == -1) ? content.substring(1) : content.substring(1, splitIndex);

        return registeredActionHandlers.containsKey(firstToken.toUpperCase());
    }

    private static ActionHandler parseAction(String content, Map<String, ActionHandler> registeredActionHandlers) {
        int splitIndex = content.indexOf(' ');
        String firstToken = (splitIndex == -1) ? content.substring(1) : content.substring(1, splitIndex);
        String firstTokenAction = firstToken.toUpperCase();

        return registeredActionHandlers.get(firstTokenAction);
    }

    private static int parseEscapeCharacter(String content, ProcessingContext processingContext) {
        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
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
                int nextSpaceIndex = content.indexOf(' ', secondSpaceIndex + 1);
                String invalidEscapeCharacter = nextSpaceIndex != -1
                        ? content.substring(secondSpaceIndex + 1, nextSpaceIndex)
                        : content.substring(secondSpaceIndex + 1);
                processingContext.addMessages(
                        MessageFormat.format("The defined escape character \"{0}\" is not valid - it must be a single character", invalidEscapeCharacter),
                        ProcessingContext.MessageType.PARSING_ERROR
                );
                return -1;
            }
        }

        if (!ChatCommand.VALID_ESCAPE_CHARACTERS.contains(potentialEscapeCharacter)) {
            processingContext.addMessages(
                    MessageFormat.format(
                            "The defined escape character \"{0}\" is not valid, valid characters are: {1}",
                            potentialEscapeCharacter,
                            Helper.stringifyCollection(ChatCommand.VALID_ESCAPE_CHARACTERS, String::toString, false)
                    ),
                    ProcessingContext.MessageType.PARSING_ERROR
            );
            return -1;
        }

        return secondSpaceIndex + 1;
    }

    private static void processMentions(Message message, ProcessingContext processingContext) {
        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
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
        if (processingContext.hasMessageOfType(ProcessingContext.MessageType.PARSING_ERROR)) {
            return null;
        }


        IndexedMessage indexedMessage = new IndexedMessage();

        int i = (escapeCharacterIndex != -1) ? escapeCharacterIndex + 2 : content.indexOf(' ') + 1;
        Character escapeCharacter = (escapeCharacterIndex == -1) ? null : content.charAt(escapeCharacterIndex);

        boolean escapeNext = false;
        boolean inSentence = false;

        for (; i < content.length(); ++i) {
            char c = content.charAt(i);

            if (escapeNext) {
                escapeNext = false;
                ChatCommand.warnIfEscapeWasUseless(c, escapeCharacter, processingContext);

                if (c == ' ' && !inSentence) {
                    IndexedMessage.TypedCharacter previousTypedCharacter = indexedMessage.getLast();
                    if (previousTypedCharacter != null && previousTypedCharacter.character() != ' ') {
                        indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                    }
                } else {
                    indexedMessage.addCharacter(c, IndexedMessage.TypedCharacter.CharacterType.LITERAL);
                }

                continue;
            }

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

            if (escapeCharacter != null && escapeCharacter.equals(c)) {
                escapeNext = true;
                continue;
            }

            if (ChatCommand.MODIFIER_PREFIX.equals(String.valueOf(c))) {
                char previous = content.charAt(i - 1);
                if ((previous == ' ' || previous == '\n') && !inSentence) {
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
                        if (next == ' ' || next == '\n') {
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
                    if (previous == ' ' || previous == '\n') {
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
        if (argument.isEmpty()) {
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

    public<T extends Enum<T>> TypedValue getFirstArgument(
            T modifier, boolean allowNullArgument, boolean setUsed, ProcessingContext processingContext
    ) {
        // Ignore invalid resolution warnings and check first argument only
        ProcessingContext dummy = new ProcessingContext();
        List<TypedValue> arguments = this.getArguments(modifier, allowNullArgument, false, dummy);
        ChatCommand.warnIfResolutionIsNotValidArgument(modifier, arguments.getFirst(), processingContext);

        if (setUsed) {
            arguments.getFirst().setUsed();
        }

        return arguments.getFirst();
    }

    public<T extends Enum<T>, U extends Enum<U>> U getFirstArgumentAsEnum(
            T modifier, Class<U> requiredEnumClass, boolean setUsed, ProcessingContext processingContext
    ) {
        List<TypedValue> arguments = this.modifierMap.get(modifier);
        TypedValue firstArgument = arguments.getFirst();

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

        ChatCommand.warnIfResolutionIsNotValidArgument(modifier, firstArgument, processingContext);

        if (setUsed) {
            firstArgument.setUsed();
        }

        return requiredEnumClass.cast(Enum.valueOf(requiredEnumClass, firstArgument.getUsedValue().toUpperCase()));
    }

    public<T extends Enum<T>> List<TypedValue> getArguments(
            T modifier, boolean allowNullArgument, boolean setUsed, ProcessingContext processingContext
    ) {
        List<TypedValue> arguments = this.modifierMap.get(modifier);

        if (!allowNullArgument) {
            if (arguments.getFirst().getType() == TypedValue.Type.NULL) {
                throw new MissingArgumentException(arguments.getFirst().getStateMessage(modifier.toString(), false));
            }
        }

        arguments.forEach(argument -> {
            ChatCommand.warnIfResolutionIsNotValidArgument(modifier, argument, processingContext);

            if (setUsed) {
                argument.setUsed();
            }
        });

        if (arguments.size() == 1 && arguments.getFirst().getType() == TypedValue.Type.NULL) {
            return new ArrayList<>();
        }

        return arguments;
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

    public ActionHandler getActionHandler() {
        return this.actionHandler;
    }

    public ModifierMap getModifierMap() {
        return this.modifierMap;
    }
}
