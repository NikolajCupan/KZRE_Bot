package org;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProcessingContext {
    public enum MessageType { RESULT, SUCCESS, WARNING, ERROR }
    public record Message(String message, MessageType messageType) {}

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
                .collect(Collectors.toList());
    }

    public boolean hasErrorMessage() {
        return this.messages.stream().anyMatch(element ->
            element.messageType == MessageType.ERROR
        );
    }
}
