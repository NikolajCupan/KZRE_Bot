package org;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.database.dto.GuildDto;
import org.database.dto.UserDto;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.utility.Constants;
import org.utility.ProcessingContext;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class MessageListener extends ListenerAdapter {
    protected static void returnResponse(MessageChannel channel, EmbedBuilder embedBuilder, boolean keepHeaders) {
        EmbedBuilder processedEmbedBuilder = new EmbedBuilder(embedBuilder);

        if (!keepHeaders) {
            processedEmbedBuilder.clearFields();
            embedBuilder.getFields().forEach(field -> {
                assert field.getValue() != null;
                processedEmbedBuilder.addField("", field.getValue(), false);
            });
        }

        if (!processedEmbedBuilder.isEmpty()) {
            channel.sendMessageEmbeds(processedEmbedBuilder.build()).queue();
        }
    }

    private static void beforeMessageProcessed(MessageReceivedEvent event) {
        Session session = Main.DATABASE_SESSION_FACTORY.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            GuildDto.refreshGuild(event.getGuild().getId(), session);
            UserDto.refreshUser(event.getAuthor().getId(), session);
        } finally {
            transaction.commit();
            session.close();
        }
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

        Request request = new Request(event.getChannel().getId(), event.getAuthor().getId(), event.getGuild().getId());
        boolean verboseResponse = false;
        Object lock = request.acquireLock();
        if (lock != null) {
            synchronized (lock) {
                try {
                    MessageListener.beforeMessageProcessed(event);
                    verboseResponse = this.processMessage(event, processingContext);
                    MessageListener.afterMessageProcessed(embedBuilder, processingContext, verboseResponse);
                } finally {
                    request.releaseLock();
                }
            }
        } else {
            processingContext.addMessages("Request could not be processed", ProcessingContext.MessageType.ERROR);
        }

        MessageListener.returnResponse(event.getChannel(), embedBuilder, verboseResponse);
    }

    public static class Request {
        private static final Map<Pair<String, String>, Request.Lock> LOCKS;

        static {
            LOCKS = Collections.synchronizedMap(new HashMap<>());
        }

        private final Pair<String, String> key;
        private final String guildId;
        private boolean lockAquired;

        public Request(String channelId, String userId, String guildId) {
            this.key = new Pair<>(channelId, userId);
            this.guildId = guildId;
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

        public String getGuildId() {
            return this.guildId;
        }

        public static class PendingListenersRemovalsComparator implements Comparator<Request> {
            @Override
            public int compare(Request lhs, Request rhs) {
                int comparison = lhs.getKey().getValue0().compareTo(rhs.getKey().getValue0());
                return comparison != 0 ? comparison : lhs.getKey().getValue1().compareTo(rhs.getKey().getValue1());
            }
        }

        public static class GuildUserLockComparator implements Comparator<Request> {
            @Override
            public int compare(Request lhs, Request rhs) {
                int comparison = lhs.getKey().getValue1().compareTo(rhs.getKey().getValue1());
                return comparison != 0 ? comparison : lhs.getGuildId().compareTo(rhs.getGuildId());
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
