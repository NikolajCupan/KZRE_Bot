package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.parser.ChatCommand;

public abstract class ActionHandler {
    public abstract Action getAction();
    public abstract void executeAction(MessageReceivedEvent event, ChatCommand chatCommand);

    @Override
    public String toString() {
        return this.getAction().toString();
    }
}
