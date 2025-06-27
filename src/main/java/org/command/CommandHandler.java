package org.command;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class CommandHandler {
    public abstract String getCommand();
    public abstract void executeAction(MessageReceivedEvent event);

    @Override
    public String toString() {
        return this.getCommand();
    }
}
