package course.concurrency.m6;

import course.concurrency.m6.queue.blocking.BlockingQueue;
import course.concurrency.m6.queue.blocking.FixedSizeBlockingQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FixedSizeBlockingQueueTest {

    public static final String NOT_EMPTY_MSG = "Queue is not empty";
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

    /**
     * Single-thread tests
     */

    @Test
    public void shouldEnqueueFixedCount() {
        int capacity = 5;
        course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        List<Integer> putValues = enqueueRandomIntegers(capacity * 2, queue);

        for (int i = 0; i < capacity; ++i) {
            Assertions.assertEquals(putValues.get(i), queue.dequeue(), "Unexpected queue element");
        }

        for (int i = capacity; i < putValues.size(); ++i) {
            assertNull(queue.dequeue(), NOT_EMPTY_MSG);
        }
    }

    @Test
    public void testFillingQueueTwice() {
        int capacity = 5;
        BlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        enqueueRandomIntegers(capacity, queue);
        dequeueValues(capacity, queue);
        assertNull(queue.dequeue(), NOT_EMPTY_MSG);

        int count = 1000;
        List<Integer> putValues = enqueueRandomIntegers(count, queue);

        for (int i = 0; i < capacity; ++i) {
            Assertions.assertEquals(putValues.get(i), queue.dequeue(), "Unexpected queue element");
        }
        assertNull(queue.dequeue(), NOT_EMPTY_MSG);
    }

    /**
     * Multithreading tests
     */

    @Test
    public void shouldEnqueueAllExpectedValuesParallel() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int capacity = 100000;
        course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

        int threads = POOL_SIZE;
        int elementsPerThread = capacity / threads;

        Map<Integer, Long> expectedOccurrences = new HashMap<>();
        for (int i = 0; i < threads; ++i) {
            int value = i;
            executor.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException ignored) {
                }
                enqueueSameIntegers(elementsPerThread, value, queue);
            });
            expectedOccurrences.put(value, (long) elementsPerThread);
        }
        countDownLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Not terminated during the expected time");

        Map<Integer, Long> occurrences = IntStream.range(0, elementsPerThread * threads)
                .mapToObj(i -> queue.dequeue())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(occurrences)
                .as("Check that none values are lost")
                .containsExactlyEntriesOf(expectedOccurrences);
    }

    @ParameterizedTest
    @ValueSource(ints = {32, 100000, 10000000})
    public void checkEnqueuedElementsCountParallel(int capacity) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

        int threads = POOL_SIZE;
        int elementsPerThread = capacity / threads;

        for (int i = 0; i < threads; ++i) {
            executor.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException ignored) {
                }
                enqueueRandomIntegers(elementsPerThread, queue);

                int halfOfAddedElements = elementsPerThread / 2;
                dequeueValues(halfOfAddedElements, queue);

                enqueueRandomIntegers(halfOfAddedElements, queue);
            });
        }
        countDownLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(100, TimeUnit.SECONDS),
                "Not terminated during the expected time");

        assertTrue(IntStream.range(0, elementsPerThread * threads)
                .mapToObj(i -> queue.dequeue())
                .noneMatch(Objects::isNull), "Not all elements were enqueued");

        assertNull(queue.dequeue(), NOT_EMPTY_MSG);
    }

    @Test
    public void checkNotTooMuchElementsEnqueuedParallel() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int capacity = 10000;
        course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

        int threads = POOL_SIZE;
        int elementsPerThread = capacity / threads;

        for (int i = 0; i < threads; ++i) {
            executor.submit(() -> {
                try {
                    countDownLatch.await();
                } catch (InterruptedException ignored) {
                }
                enqueueRandomIntegers(elementsPerThread / 2, queue);

                dequeueValues(elementsPerThread / 4, queue);

                enqueueRandomIntegers(elementsPerThread, queue);
            });
        }
        countDownLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(100, TimeUnit.SECONDS),
                "Not terminated during the expected time");

        assertTrue(IntStream.range(0, capacity)
                .mapToObj(i -> queue.dequeue())
                .noneMatch(Objects::isNull),
                "Queue has not enough elements (should be filled up to full capacity)");

        assertNull(queue.dequeue(), NOT_EMPTY_MSG);
    }

    /**
     * Utility methods
     */

    private List<Integer> enqueueRandomIntegers(int count, course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue) {
        List<Integer> putValues = new ArrayList<>();
        threadLocalRandom.ints(count).forEach(i -> {
            queue.enqueue(i);
            putValues.add(i);
        });
        return putValues;
    }

    private void enqueueSameIntegers(int count, Integer value, course.concurrency.m6.queue.blocking.BlockingQueue<Integer> queue) {
        threadLocalRandom.ints(count).forEach(i -> queue.enqueue(value));
    }

    private void dequeueValues(int count, BlockingQueue<Integer> queue) {
        IntStream.range(0, count).forEach(i -> queue.dequeue());
    }

}
