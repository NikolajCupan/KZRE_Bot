package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.action.ConfirmationMessageListener;
import org.database.GuildManager;
import org.database.Persistable;
import org.database.UserManager;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.utility.Constants;
import org.utility.ProcessingContext;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MessageListener extends ListenerAdapter {
    private static final UserManager USER_MANAGER;// TODO: remove this class
    private static final GuildManager GUILD_MANAGER;// TODO: remove this class
    private static final Map<ConfirmationKey, ScheduledFuture<?>> PENDING_LISTENERS_REMOVALS;

    private static final ScheduledExecutorService EXECUTOR_SERVICE;

    static {
        USER_MANAGER = new UserManager();
        GUILD_MANAGER = new GuildManager();
        PENDING_LISTENERS_REMOVALS = Collections.synchronizedMap(new TreeMap<>(new MessageListener.ConfirmationKey.ConfirmationKeyComparator()));

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        executor.setRemoveOnCancelPolicy(true);
        EXECUTOR_SERVICE = Executors.unconfigurableScheduledExecutorService(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(MessageListener.EXECUTOR_SERVICE::shutdownNow));
    }

    public static void addConfirmationMessageListener(MessageReceivedEvent event, Persistable objectToStore) {
        ConfirmationKey confirmationKey = new ConfirmationKey(event.getChannel().getId(), event.getAuthor().getId());

        synchronized (confirmationKey.acquireLock()) {
            try {
                if (MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(confirmationKey)) {
                    throw new RuntimeException("The confirmation key is already present in the set");
                }

                ConfirmationMessageListener confirmationMessageListener =
                        new ConfirmationMessageListener(event.getChannel().getId(), event.getAuthor().getId(), objectToStore);

                Main.JDA_API.addEventListener(confirmationMessageListener);
                ScheduledFuture<?> scheduledFuture = MessageListener.EXECUTOR_SERVICE.schedule(
                        () -> MessageListener.removeConfirmationMessageListener(confirmationMessageListener), 5000, TimeUnit.MILLISECONDS
                );
                MessageListener.PENDING_LISTENERS_REMOVALS.put(confirmationKey, scheduledFuture);
            } finally {
                confirmationKey.releaseLock();
            }
        }
    }

    public static void removeConfirmationMessageListener(ConfirmationMessageListener messageListener) {
        ConfirmationKey confirmationKey = new ConfirmationKey(messageListener.getChannelId(), messageListener.getUserId());

        synchronized (confirmationKey.acquireLock()) {
            try {
                if (!MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(confirmationKey)) {
                    return;
                    // throw new RuntimeException("The confirmation key is not present in the set");
                }

                ScheduledFuture<?> scheduledFuture = MessageListener.PENDING_LISTENERS_REMOVALS.remove(confirmationKey);
                scheduledFuture.cancel(false);
                Main.JDA_API.removeEventListener(messageListener);
            } finally {
                confirmationKey.releaseLock();
            }
        }
    }

    public static boolean confirmationKeyExists(String channelId, String userId) {
        ConfirmationKey confirmationKey = new ConfirmationKey(channelId, userId);
        synchronized (confirmationKey.acquireLock()) {
            try {
                return MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(new ConfirmationKey(channelId, userId));
            } finally {
                confirmationKey.releaseLock();
            }
        }
    }

    private static void beforeMessageProcessed(MessageReceivedEvent event) {
        MessageListener.USER_MANAGER.refreshUser(event);
        MessageListener.GUILD_MANAGER.refreshGuild(event);
    }

    private static void afterMessageProcessed(
            MessageReceivedEvent event, EmbedBuilder embedBuilder, ProcessingContext processingContext, boolean verboseResponse
    ) {
        if (processingContext.hasParsingErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        } else if (processingContext.hasErrorMessage()) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.ERROR)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        } else {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.INFO_RESULT, ProcessingContext.MessageType.SUCCESS_RESULT)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }

        if (verboseResponse) {
            processingContext.getMessages(List.of(ProcessingContext.MessageType.PARSING_WARNING, ProcessingContext.MessageType.WARNING)).forEach(element ->
                    embedBuilder.addField(element.messageType().toString(), element.message(), false)
            );
        }


        if (!embedBuilder.isEmpty()) {
            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

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
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.BLACK);

        MessageListener.beforeMessageProcessed(event);
        boolean verboseResponse = this.processMessage(event, processingContext);
        MessageListener.afterMessageProcessed(event, embedBuilder, processingContext, verboseResponse);
    }

    private static class ConfirmationKey {
        private static final Map<Pair<String, String>, ConfirmationKey.Lock> LOCKS;

        static {
            LOCKS = Collections.synchronizedMap(new HashMap<>());
        }

        private final Pair<String, String> key;

        public ConfirmationKey(String channelId, String userId) {
            this.key = new Pair<>(channelId, userId);
        }

        public Object acquireLock() {
            return ConfirmationKey.LOCKS.computeIfAbsent(this.key, _ -> new Lock()).getLock();
        }

        public void releaseLock() {
            ConfirmationKey.LOCKS.computeIfPresent(this.key, (_, lock) -> {
                if (lock.getReferenceCount().decrementAndGet() <= 0) {
                    return null;
                }

                return lock;
            });
        }

        public Pair<String, String> getKey() {
            return this.key;
        }

        public static class ConfirmationKeyComparator implements Comparator<ConfirmationKey> {
            @Override
            public int compare(ConfirmationKey lhs, ConfirmationKey rhs) {
                int comparison = lhs.getKey().getValue0().compareTo(rhs.getKey().getValue0());
                return comparison != 0 ? comparison : lhs.getKey().getValue1().compareTo(rhs.getKey().getValue1());
            }
        }

        private static class Lock {
            private final AtomicInteger referenceCount;
            private final Object lock;

            public Lock() {
                this.referenceCount = new AtomicInteger(0);
                this.lock = new Object();
            }

            public AtomicInteger getReferenceCount() {
                return this.referenceCount;
            }

            public Object getLock() {
                this.referenceCount.incrementAndGet();
                return this.lock;
            }
        }
    }
}
