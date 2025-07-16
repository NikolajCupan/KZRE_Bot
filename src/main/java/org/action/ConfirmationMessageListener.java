package org.action;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Main;
import org.MessageListener;
import org.database.Persistable;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.parsing.ChatConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.ProcessingContext;

import java.text.MessageFormat;

public class ConfirmationMessageListener extends MessageListener {
    private static final Logger LOGGER;

    static {
        LOGGER = LoggerFactory.getLogger(ConfirmationMessageListener.class);
    }

    private final String channelId;
    private final String userId;
    private final Persistable objectToStore;

    public ConfirmationMessageListener(String channelId, String userId, Persistable objectToStore) {
        this.channelId = channelId;
        this.userId = userId;
        this.objectToStore = objectToStore;
    }

    @Override
    protected boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext) {
        if (!event.getChannel().getId().equals(this.channelId)
                || !event.getAuthor().getId().equals(this.userId)) {
            return false;
        }


        ConfirmationMessageListener.LOGGER.info("Received message \"{}\"", event.getMessage().getContentRaw());

        ChatConfirmation chatConfirmation = new ChatConfirmation(event.getMessage());
        switch (chatConfirmation.getStatus()) {
            case ChatConfirmation.Status.YES -> this.handleConfirm(processingContext);
            case ChatConfirmation.Status.NO -> this.handleDecline(processingContext);
            case ChatConfirmation.Status.INVALID -> this.handleInvalidOption(processingContext);
        }

        return false;
    }

    private void handleConfirm(ProcessingContext processingContext) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            this.objectToStore.persist(processingContext, session);
        } finally {
            MessageListener.removeConfirmationMessageListener(this);
            transaction.commit();
            session.close();
        }
    }

    private void handleDecline(ProcessingContext processingContext) {
        this.objectToStore.rejectPersist(processingContext);
        MessageListener.removeConfirmationMessageListener(this);
    }

    private void handleInvalidOption(ProcessingContext processingContext) {
        processingContext.addMessages(
                MessageFormat.format(
                        "Unknown confirmation value provided, please use \"{0}\"/\"{1}\"",
                        ChatConfirmation.Status.YES.toString(),
                        ChatConfirmation.Status.NO.toString()
                ),
                ProcessingContext.MessageType.ERROR
        );
    }

    public String getChannelId() {
        return this.channelId;
    }

    public String getUserId() {
        return this.userId;
    }
}
