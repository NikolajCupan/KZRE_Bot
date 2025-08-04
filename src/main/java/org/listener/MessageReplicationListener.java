package org.listener;

import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MessageReplicationListener extends ListenerAdapter {
    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {}
}
