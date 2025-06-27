package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.parser.Command;

public abstract class ActionHandler {
    public abstract String getAction();
    public abstract void executeAction(MessageReceivedEvent event, Command command);

    @Override
    public String toString() {
        return this.getAction();
    }
}
