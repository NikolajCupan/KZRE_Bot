package org.command;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Quote extends CommandHandler {
    @Override
    public String getCommand() {
        return "quote";
    }

    @Override
    public void executeAction(MessageReceivedEvent event) {
        MessageChannel channel = event.getChannel();
        channel.sendMessage("test").queue();
    }
}
