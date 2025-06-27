package org.action;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.parser.Command;

public class Quote extends ActionHandler {
    @Override
    public String getAction() {
        return "quote";
    }

    @Override
    public void executeAction(MessageReceivedEvent event, Command command) {
        MessageChannel channel = event.getChannel();
        channel.sendMessage("test").queue();
    }
}
