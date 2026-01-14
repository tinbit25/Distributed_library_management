package util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements Lamport Logical Clock for event ordering in distributed systems.
 */
public class LamportClock {
    private final AtomicInteger counter = new AtomicInteger(0);

    public int getValue() {
        return counter.get();
    }

    public int tick() {
        return counter.incrementAndGet();
    }

    /**
     * Update the clock upon receiving a message with a timestamp.
     * Rule: L(e) = max(local_clock, received_timestamp) + 1
     */
    public void update(int receivedValue) {
        int current;
        int next;
        do {
            current = counter.get();
            next = Math.max(current, receivedValue) + 1;
        } while (!counter.compareAndSet(current, next));
    }
}
