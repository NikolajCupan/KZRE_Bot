package org.action.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ActionRequest {
    private static final Map<ActionRequest.RequestKey, Lock> LOCKS;

    static {
        LOCKS = Collections.synchronizedMap(new HashMap<>());
    }

    private final ActionRequest.RequestKey key;
    private final String guildId;
    private boolean lockAquired;

    public ActionRequest(String channelId, String userId, String guildId) {
        this.key = new ActionRequest.RequestKey(channelId, userId);
        this.guildId = guildId;
        this.lockAquired = false;
    }

    public Object acquireLock() {
        if (this.lockAquired) {
            throw new IllegalStateException("Lock for this request was already aquired");
        }

        Object lock = ActionRequest.LOCKS.computeIfAbsent(this.key, _ -> new Lock()).getLock();
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

        ActionRequest.LOCKS.computeIfPresent(this.key, (_, lock) -> {
            if (lock.returnLock() <= 0) {
                return null;
            }

            return lock;
        });
    }

    public ActionRequest.RequestKey getKey() {
        return this.key;
    }

    public String getGuildId() {
        return this.guildId;
    }

    public static class PendingListenersRemovalsComparator implements Comparator<ActionRequest> {
        @Override
        public int compare(ActionRequest lhs, ActionRequest rhs) {
            int comparison = lhs.getKey().channelId().compareTo(rhs.getKey().channelId());
            return comparison != 0 ? comparison : lhs.getKey().userId().compareTo(rhs.getKey().userId());
        }
    }

    public static class GuildUserLockComparator implements Comparator<ActionRequest> {
        @Override
        public int compare(ActionRequest lhs, ActionRequest rhs) {
            int comparison = lhs.getKey().userId().compareTo(rhs.getKey().userId());
            return comparison != 0 ? comparison : lhs.getGuildId().compareTo(rhs.getGuildId());
        }
    }

    public record RequestKey(String channelId, String userId) {}

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
