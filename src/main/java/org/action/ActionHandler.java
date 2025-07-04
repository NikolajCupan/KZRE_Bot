package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.ProcessingContext;
import org.parser.ChatCommand;

import java.util.Set;

public abstract class ActionHandler {
    public abstract Action getAction();
    public abstract Set<String> getActionModifiersKeywords();
    public abstract void executeAction(MessageReceivedEvent event, ChatCommand chatCommand, ProcessingContext processingContext);

    @Override
    public String toString() {
        return this.getAction().toString();
    }
}
