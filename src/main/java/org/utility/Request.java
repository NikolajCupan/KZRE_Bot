package org.utility;

import org.javatuples.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class Request {
    private static final Map<Pair<String, String>, Lock> LOCKS;

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
