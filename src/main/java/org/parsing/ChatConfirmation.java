package org.parsing;

import net.dv8tion.jda.api.entities.Message;

public class ChatConfirmation {
    private final Status status;

    public ChatConfirmation(Message message) {
        String content = message.getContentRaw();

        if (content.equalsIgnoreCase(ChatConfirmation.Status.YES.toString())) {
            this.status = ChatConfirmation.Status.YES;
        } else if (content.equalsIgnoreCase(ChatConfirmation.Status.NO.toString())) {
            this.status = ChatConfirmation.Status.NO;
        } else {
            this.status = ChatConfirmation.Status.INVALID;
        }
    }

    public Status getStatus() {
        return this.status;
    }

    public enum Status { YES, NO, INVALID }
}
