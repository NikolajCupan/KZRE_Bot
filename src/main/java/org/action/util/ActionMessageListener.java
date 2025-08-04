package org.action.util;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.Main;
import org.jetbrains.annotations.NotNull;
import org.utility.Constants;
import org.utility.Helper;
import org.utility.ProcessingContext;
import org.utility.CommunicationStream;

public abstract class ActionMessageListener extends ListenerAdapter {
    protected abstract boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext);

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (Main.COMMAND_LINE_ARGUMENTS.contains(Constants.DEVELOPMENT_ARGUMENT)
                && !event.getGuild().getId().equals(Constants.DEVELOPMENT_SERVER_ID)) {
            return;
        }

        if (event.getAuthor().isBot()) {
            return;
        }


        ProcessingContext processingContext = new ProcessingContext();
        ActionRequest actionRequest = new ActionRequest(event.getChannel().getId(), event.getAuthor().getId(), event.getGuild().getId());
        boolean verboseResponse = false;

        Object lock = actionRequest.acquireLock();
        if (lock != null) {
            synchronized (lock) {
                try {
                    Helper.refreshGuildAndUser(event);
                    verboseResponse = this.processMessage(event, processingContext);
                } finally {
                    actionRequest.releaseLock();
                }
            }
        } else {
            processingContext.addMessages("Action request could not be processed", ProcessingContext.MessageType.ERROR);
        }

        if (verboseResponse) {
            EmbedBuilder response = CommunicationStream.processRequestResultVerbose(processingContext);
            CommunicationStream.returnResponseVerbose(event.getChannel(), response);
        } else {
            String response = CommunicationStream.processRequestResult(processingContext);
            CommunicationStream.returnResponse(event.getChannel(), response);
        }
    }
}
