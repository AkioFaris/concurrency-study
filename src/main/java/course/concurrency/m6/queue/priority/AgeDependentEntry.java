package course.concurrency.m6.queue.priority;

import lombok.Getter;

import java.time.Duration;

public class AgeDependentEntry<E extends Comparable<? super E>> implements Comparable<AgeDependentEntry<E>> {
    private final long timestamp;
    private final long maxAgeInMillis;

    @Getter
    private final E entry;

    public AgeDependentEntry(E entry, long maxAgeInMillis) {
        this.timestamp = System.currentTimeMillis();
        this.entry = entry;
        this.maxAgeInMillis = maxAgeInMillis;
    }

    public AgeDependentEntry(E entry, Duration maxAgeDuration) {
        this.timestamp = System.currentTimeMillis();
        this.entry = entry;
        this.maxAgeInMillis = maxAgeDuration.toMillis();
    }

    public int compareTo(AgeDependentEntry<E> other) {
        if (System.currentTimeMillis() - timestamp >= maxAgeInMillis) {
            return -Integer.MAX_VALUE;
        }
        return entry.compareTo(other.entry);
    }

}
