package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
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
    private static final Map<Request, ScheduledFuture<?>> PENDING_LISTENERS_REMOVALS;

    private static final ScheduledExecutorService EXECUTOR_SERVICE;

    static {
        USER_MANAGER = new UserManager();
        GUILD_MANAGER = new GuildManager();
        PENDING_LISTENERS_REMOVALS = Collections.synchronizedMap(new TreeMap<>(new Request.ConfirmationKeyComparator()));

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);
        executor.setRemoveOnCancelPolicy(true);
        EXECUTOR_SERVICE = Executors.unconfigurableScheduledExecutorService(executor);
        Runtime.getRuntime().addShutdownHook(new Thread(MessageListener.EXECUTOR_SERVICE::shutdownNow));
    }

    public static void removeConfirmationMessageListener(ConfirmationMessageListener messageListener) {
        Request request = new Request(messageListener.getChannelId(), messageListener.getUserId());

        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageListener) {
            if (!MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(request)) {
                // listener was already removed
                return;
            }

            ScheduledFuture<?> scheduledFuture = MessageListener.PENDING_LISTENERS_REMOVALS.remove(request);
            scheduledFuture.cancel(true);
            Main.JDA_API.removeEventListener(messageListener);
        }
    }

    private static void removeConfirmationMessageListenerAfterTimeout(ConfirmationMessageListener messageListener) {
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (messageListener) {
            if (Thread.interrupted()) {
                // listener was already removed
                return;
            }

            MessageListener.removeConfirmationMessageListener(messageListener);
            MessageListener.returnResponse(
                    Main.JDA_API.getChannelById(MessageChannel.class, messageListener.getChannelId()),
                    new EmbedBuilder().setColor(Color.BLACK)
                            .addField(ProcessingContext.MessageType.ERROR.toString(), "Confirmation timed out", false)
            );
        }
    }

    public static void addConfirmationMessageListener(MessageReceivedEvent event, Persistable objectToStore) {
        Request request = new Request(event.getChannel().getId(), event.getAuthor().getId());
        if (MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(request)) {
            throw new RuntimeException("The key is already present in the set");
        }

        ConfirmationMessageListener confirmationMessageListener =
                new ConfirmationMessageListener(event.getChannel().getId(), event.getAuthor().getId(), objectToStore);

        Main.JDA_API.addEventListener(confirmationMessageListener);
        ScheduledFuture<?> scheduledFuture = MessageListener.EXECUTOR_SERVICE.schedule(
                () -> MessageListener.removeConfirmationMessageListenerAfterTimeout(confirmationMessageListener), 5000, TimeUnit.MILLISECONDS
        );
        MessageListener.PENDING_LISTENERS_REMOVALS.put(request, scheduledFuture);
    }

    public static boolean confirmationKeyExists(String channelId, String userId) {
        return MessageListener.PENDING_LISTENERS_REMOVALS.containsKey(new Request(channelId, userId));
    }

    private static void beforeMessageProcessed(MessageReceivedEvent event) {
        MessageListener.USER_MANAGER.refreshUser(event);
        MessageListener.GUILD_MANAGER.refreshGuild(event);
    }

    private static void afterMessageProcessed(
            EmbedBuilder embedBuilder, ProcessingContext processingContext, boolean verboseResponse
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
    }

    private static void returnResponse(MessageChannel channel, EmbedBuilder embedBuilder) {
        if (!embedBuilder.isEmpty()) {
            channel.sendMessageEmbeds(embedBuilder.build()).queue();
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

        Request request = new Request(event.getChannel().getId(), event.getAuthor().getId());
        Object lock = request.acquireLock();
        if (lock != null) {
            synchronized (lock) {
                try {
                    MessageListener.beforeMessageProcessed(event);
                    boolean verboseResponse = this.processMessage(event, processingContext);
                    MessageListener.afterMessageProcessed(embedBuilder, processingContext, verboseResponse);
                } finally {
                    request.releaseLock();
                }
            }
        } else {
            processingContext.addMessages("Request could not be processed", ProcessingContext.MessageType.ERROR);
        }

        MessageListener.returnResponse(event.getChannel(), embedBuilder);
    }

    private static class Request {
        private static final Map<Pair<String, String>, Request.Lock> LOCKS;

        static {
            LOCKS = Collections.synchronizedMap(new HashMap<>());
        }

        private final Pair<String, String> key;
        private boolean lockAquired;

        public Request(String channelId, String userId) {
            this.key = new Pair<>(channelId, userId);
            this.lockAquired = false;
        }

        public Object acquireLock() {
            if (this.lockAquired) {
                throw new IllegalStateException("Lock for this request was already aquired");
            }

            Object lock = Request.LOCKS.computeIfAbsent(this.key, _ -> new Lock()).getLock();
            if (lock != null) {
                this.lockAquired = true;
                return lock;
            }

            return null;
        }

        public void releaseLock() {
            if (!this.lockAquired) {
                throw new IllegalStateException("Lock for this request was not aquired");
            }

            Request.LOCKS.computeIfPresent(this.key, (_, lock) -> {
                if (lock.returnLock() <= 0) {
                    return null;
                }

                return lock;
            });
        }

        public Pair<String, String> getKey() {
            return this.key;
        }

        public static class ConfirmationKeyComparator implements Comparator<Request> {
            @Override
            public int compare(Request lhs, Request rhs) {
                int comparison = lhs.getKey().getValue0().compareTo(rhs.getKey().getValue0());
                return comparison != 0 ? comparison : lhs.getKey().getValue1().compareTo(rhs.getKey().getValue1());
            }
        }

        private static class Lock {
            private static final int MAXIMUM_NUMBER_OF_LOCKS;

            static {
                MAXIMUM_NUMBER_OF_LOCKS = 5;
            }

            private final AtomicInteger referenceCount;
            private final Object lock;

            public Lock() {
                this.referenceCount = new AtomicInteger(0);
                this.lock = new Object();
            }

            public synchronized int getReferenceCount() {
                return this.referenceCount.get();
            }

            public synchronized Object getLock() {
                if (this.referenceCount.get() >= Lock.MAXIMUM_NUMBER_OF_LOCKS) {
                    return null;
                }

                this.referenceCount.incrementAndGet();
                return this.lock;
            }

            public synchronized int returnLock() {
                return this.referenceCount.decrementAndGet();
            }
        }
    }
}
