package org.utility;

import java.util.ArrayList;
import java.util.List;

public class ProcessingContext {
    private final List<Message> messages;

    public ProcessingContext() {
        this.messages = new ArrayList<>();
    }

    public void addMessages(String message, MessageType messageType) {
        this.messages.add(new Message(message, messageType));
    }

    public List<Message> getMessages(List<MessageType> messageTypes) {
        return this.messages.stream()
                .filter(element -> messageTypes.contains(element.messageType))
                .toList();
    }

    public boolean hasErrorMessage() {
        return this.messages.stream().anyMatch(element ->
            element.messageType == MessageType.ERROR
        );
    }

    public boolean hasParsingErrorMessage() {
        return this.messages.stream().anyMatch(element ->
                element.messageType == MessageType.PARSING_ERROR
        );
    }

    public enum MessageType { INFO_RESULT, SUCCESS_RESULT, WARNING, PARSING_WARNING, ERROR, PARSING_ERROR }
    public record Message(String message, MessageType messageType) {}
}
