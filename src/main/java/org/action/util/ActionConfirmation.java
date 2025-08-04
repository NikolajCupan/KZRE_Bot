package org.action.util;

import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.Main;
import org.database.Persistable;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.parsing.ChatConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.utility.ProcessingContext;
import org.utility.CommunicationStream;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;

public class ActionConfirmation extends ActionMessageListener {
    private static final Logger LOGGER;

    private static final int CONFIRMATION_TIMEOUT_S;
    private static final Map<ActionRequest, ScheduledFuture<?>> PENDING_LISTENERS_REMOVALS;
    private static final Map<ActionRequest, String> GUILD_USER_LOCKS;
    private static final ScheduledExecutorService EXECUTOR_SERVICE;

    static {
        LOGGER = LoggerFactory.getLogger(ActionConfirmation.class);

        CONFIRMATION_TIMEOUT_S = 20;
        PENDING_LISTENERS_REMOVALS = Collections.synchronizedMap(new TreeMap<>(new ActionRequest.PendingListenersRemovalsComparator()));
        GUILD_USER_LOCKS = Collections.synchronizedMap(new TreeMap<>(new ActionRequest.GuildUserLockComparator()));

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        EXECUTOR_SERVICE = Executors.unconfigurableScheduledExecutorService(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(ActionConfirmation.EXECUTOR_SERVICE::shutdownNow));
    }

    private final String channelId;
    private final String userId;
    private final String guildId;
    private final Persistable objectToStore;
    private Integer attemptsRemaining;

    public ActionConfirmation(String channelId, String userId, String guildId, Persistable objectToStore, Integer attemptsRemaining) {
        this.channelId = channelId;
        this.userId = userId;
        this.guildId = guildId;
        this.objectToStore = objectToStore;
        this.attemptsRemaining = attemptsRemaining;
    }

    public static void removeConfirmationMessageListener(ActionConfirmation messageListener) {
        ActionRequest actionRequest = new ActionRequest(messageListener.getChannelId(), messageListener.getUserId(), messageListener.getGuildId());

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageListener) {
            if (!ActionConfirmation.PENDING_LISTENERS_REMOVALS.containsKey(actionRequest)) {
                // listener was already removed
                return;
            }

            assert ActionConfirmation.GUILD_USER_LOCKS.containsKey(actionRequest);
            ActionConfirmation.GUILD_USER_LOCKS.remove(actionRequest);

            ScheduledFuture<?> scheduledFuture = ActionConfirmation.PENDING_LISTENERS_REMOVALS.remove(actionRequest);
            scheduledFuture.cancel(true);
            Main.JDA_API.removeEventListener(messageListener);
        }
    }

    private static void removeConfirmationMessageListenerAfterTimeout(ActionConfirmation messageListener) {
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageListener) {
            if (Thread.interrupted()) {
                // listener was already removed
                return;
            }

            ActionConfirmation.removeConfirmationMessageListener(messageListener);
            CommunicationStream.returnResponse(
                    Main.JDA_API.getChannelById(MessageChannel.class, messageListener.getChannelId()),
                    "Confirmation timed out"
            );
        }
    }

    public static int addConfirmationMessageListener(MessageReceivedEvent event, Persistable objectToStore, Integer attemptsRemaining) {
        ActionRequest actionRequest = new ActionRequest(event.getChannel().getId(), event.getAuthor().getId(), event.getGuild().getId());
        assert !ActionConfirmation.PENDING_LISTENERS_REMOVALS.containsKey(actionRequest);
        assert !ActionConfirmation.GUILD_USER_LOCKS.containsKey(actionRequest);

        ActionConfirmation actionConfirmationMessageListener =
                new ActionConfirmation(event.getChannel().getId(), event.getAuthor().getId(), event.getGuild().getId(), objectToStore, attemptsRemaining);

        Main.JDA_API.addEventListener(actionConfirmationMessageListener);
        ScheduledFuture<?> scheduledFuture = ActionConfirmation.EXECUTOR_SERVICE.schedule(
                () -> ActionConfirmation.removeConfirmationMessageListenerAfterTimeout(actionConfirmationMessageListener),
                ActionConfirmation.CONFIRMATION_TIMEOUT_S, TimeUnit.SECONDS
        );
        ActionConfirmation.PENDING_LISTENERS_REMOVALS.put(actionRequest, scheduledFuture);
        ActionConfirmation.GUILD_USER_LOCKS.put(actionRequest, event.getChannel().getName());

        return ActionConfirmation.CONFIRMATION_TIMEOUT_S;
    }

    public static boolean confirmationKeyExists(String channelId, String userId) {
        return ActionConfirmation.PENDING_LISTENERS_REMOVALS.containsKey(new ActionRequest(channelId, userId, null));
    }

    public static String getGuildUserLockedChannel(String userId, String channelId) {
        return ActionConfirmation.GUILD_USER_LOCKS.get(new ActionRequest(null, userId, channelId));
    }

    @Override
    protected boolean processMessage(MessageReceivedEvent event, ProcessingContext processingContext) {
        if (!event.getChannel().getId().equals(this.channelId)
                || !event.getAuthor().getId().equals(this.userId)) {
            return false;
        }


        ActionConfirmation.LOGGER.info("Received message \"{}\"", event.getMessage().getContentRaw());

        ChatConfirmation chatConfirmation = new ChatConfirmation(event.getMessage());
        if (this.attemptsRemaining != null) {
            --this.attemptsRemaining;
        }

        if (this.attemptsRemaining != null && this.attemptsRemaining <= 0 && chatConfirmation.getStatus() == ChatConfirmation.Status.INVALID) {
            processingContext.addMessages(
                    "Too many invalid confirmation values provided, the operation has been canceled",
                    ProcessingContext.MessageType.ERROR
            );
            this.handleDecline(processingContext);
        } else {
            switch (chatConfirmation.getStatus()) {
                case ChatConfirmation.Status.YES -> this.handleConfirm(processingContext);
                case ChatConfirmation.Status.NO -> this.handleDecline(processingContext);
                case ChatConfirmation.Status.INVALID -> this.handleInvalidOption(processingContext);
            }
        }

        return false;
    }

    private void handleConfirm(ProcessingContext processingContext) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            this.objectToStore.persist(processingContext, session);
        } finally {
            ActionConfirmation.removeConfirmationMessageListener(this);
            transaction.commit();
            session.close();
        }
    }

    private void handleDecline(ProcessingContext processingContext) {
        this.objectToStore.cancelPersist(processingContext);
        ActionConfirmation.removeConfirmationMessageListener(this);
    }

    private void handleInvalidOption(ProcessingContext processingContext) {
        String message;
        if (this.attemptsRemaining == null) {
            message = MessageFormat.format(
                    "Invalid confirmation value provided, please use \"{0}\"/\"{1}\"",
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString()
            );
        } else {
            message = MessageFormat.format(
                    "Invalid confirmation value provided, please use \"{0}\"/\"{1}\", remaining attempts: {2}",
                    ChatConfirmation.Status.YES.toString(),
                    ChatConfirmation.Status.NO.toString(),
                    this.attemptsRemaining
            );
        }

        processingContext.addMessages(message, ProcessingContext.MessageType.ERROR);
    }

    public String getChannelId() {
        return this.channelId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getGuildId() {
        return this.guildId;
    }
}
