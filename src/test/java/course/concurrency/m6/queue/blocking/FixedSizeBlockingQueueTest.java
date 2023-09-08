package course.concurrency.m6.queue.blocking;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class FixedSizeBlockingQueueTest {

    public static final String NOT_EMPTY_MSG = "Queue is not empty";
    public static final int POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public static final String WRONG_ELEMENTS_NUMBER = "Wrong elements number were added";
    public static final String WRONG_DEQUEUED_VALUE = "Unexpected queue element";
    public static final int JOIN_TIMEOUT_MILLIS = 500;
    private final ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

    /**
     * Single-thread tests
     */

    @Test
    public void checkElementsCanBeEnqueued() {
        int capacity = 5;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);
        Assertions.assertEquals(queue.size(), 0, NOT_EMPTY_MSG);

        List<Integer> putValues = enqueueRandomIntegers(capacity, queue);
        Assertions.assertEquals(queue.size(), putValues.size(), WRONG_ELEMENTS_NUMBER);
    }

    @Test
    public void checkElementCanBeDequeued() {
        int capacity = 5;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        Integer value = threadLocalRandom.nextInt();
        queue.enqueue(value);
        Assertions.assertEquals(queue.size(), 1, WRONG_ELEMENTS_NUMBER);

        Assertions.assertEquals(value, queue.dequeue(), WRONG_DEQUEUED_VALUE);
        Assertions.assertEquals(queue.size(), 0, NOT_EMPTY_MSG);
    }

    @Test
    public void checkFifoOrder() {
        int capacity = 1000;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);
        List<Integer> putValues = enqueueRandomIntegers(capacity, queue);

        for (Integer putValue : putValues) {
            Assertions.assertEquals(putValue, queue.dequeue(), WRONG_DEQUEUED_VALUE);
        }
    }

    /**
     * Multithreading tests
     */
    @Test
    public void shouldWaitAnyElementToDequeue() throws InterruptedException {
        int capacity = 10;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        log.info("First thread tries to enqueue (capacity + 1) elements");
        Thread thread1 = new Thread(() -> enqueueRandomIntegers(capacity + 1, queue));
        thread1.start();
        thread1.join(JOIN_TIMEOUT_MILLIS);
        Assertions.assertEquals(Thread.State.WAITING, thread1.getState(),
                "First thread is not waiting for queue to be not full");

        log.info("Second thread dequeues an element from the queue");
        Thread thread2 = new Thread(queue::dequeue);
        thread2.start();
        thread2.join();
        thread1.join(JOIN_TIMEOUT_MILLIS);

        log.info("Check that first thread enqueued the last element and terminated");
        Assertions.assertEquals(Thread.State.TERMINATED, thread1.getState());
    }

    @Test
    public void shouldWaitNotFullToEnqueue() throws InterruptedException {
        int capacity = 5;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        log.info("First thread tries to dequeue an element from an empty queue");
        AtomicReference<Integer> dequeued = new AtomicReference<>();
        Thread thread1 = new Thread(() -> dequeued.set(queue.dequeue()));
        thread1.start();
        thread1.join(JOIN_TIMEOUT_MILLIS);
        Assertions.assertEquals(Thread.State.WAITING, thread1.getState(),
                "First thread is not waiting for any element");

        final Integer enqueued = threadLocalRandom.nextInt();
        log.info("Second thread enqueues an element ({})", enqueued);
        Thread thread2 = new Thread(() -> queue.enqueue(enqueued));
        thread2.start();
        thread2.join();
        thread1.join(JOIN_TIMEOUT_MILLIS);

        log.info("Check that first thread got the second thread's element and terminated");
        Assertions.assertEquals(enqueued, dequeued.get(),
                "The first thread did not get value from the second thread");
        Assertions.assertEquals(Thread.State.TERMINATED, thread1.getState());
    }

    public static Stream<Arguments> loadTestDataProvider() {
        return Stream.of(
                Arguments.of(POOL_SIZE, 1000, 5),
                Arguments.of(POOL_SIZE * 2, 100000, 5),
                Arguments.of(POOL_SIZE * 3, 1000000, 20),
                Arguments.of(POOL_SIZE * 4, 10000000, 240),
                Arguments.of(POOL_SIZE, 100000000, 600)
        );
    }

    @ParameterizedTest
    @MethodSource("loadTestDataProvider")
    public void checkEnqueueDequeueInParallel(int threads, int capacity, int timeoutSec) throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

        for (int i = 0; i < threads / 2; ++i) {
            executor.submit(() -> {
                awaitForCountDown(countDownLatch);
                enqueueRandomIntegers(capacity, queue);
                dequeueValues(capacity, queue);
            });
            executor.submit(() -> {
                awaitForCountDown(countDownLatch);
                dequeueValues(capacity, queue);
                enqueueRandomIntegers(capacity, queue);
            });
        }
        countDownLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(timeoutSec, TimeUnit.SECONDS),
                "Not terminated during " + timeoutSec + " seconds");
        Assertions.assertEquals(queue.size(), 0, NOT_EMPTY_MSG);
    }

    @Test
    public void shouldEnqueueAllExpectedValuesParallel() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        int capacity = 100000;
        FixedSizeBlockingQueue<Integer> queue = new FixedSizeBlockingQueue<>(capacity);

        ExecutorService executor = Executors.newFixedThreadPool(POOL_SIZE);

        int threads = POOL_SIZE;
        int elementsPerThread = capacity / threads;

        Map<Integer, Long> expectedOccurrences = new HashMap<>();
        for (int i = 0; i < threads; ++i) {
            int value = i;
            executor.submit(() -> {
                awaitForCountDown(countDownLatch);
                enqueueSameIntegers(elementsPerThread, value, queue);
            });
            expectedOccurrences.put(value, (long) elementsPerThread);
        }
        countDownLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                "Not terminated during the expected time");

        int expectedElementsCount = elementsPerThread * threads;
        Assertions.assertEquals(queue.size(), expectedElementsCount, WRONG_ELEMENTS_NUMBER);

        Map<Integer, Long> occurrences = IntStream.range(0, expectedElementsCount)
                .mapToObj(i -> queue.dequeue())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        assertThat(occurrences)
                .as("Check that none values are lost")
                .containsExactlyEntriesOf(expectedOccurrences);
    }

    /**
     * Utility methods
     */

    private List<Integer> enqueueRandomIntegers(int count, BlockingQueue<Integer> queue) {
        List<Integer> putValues = new ArrayList<>();
        threadLocalRandom.ints(count).forEach(i -> {
            queue.enqueue(i);
            putValues.add(i);
        });
        return putValues;
    }

    private void enqueueSameIntegers(int count, Integer value, BlockingQueue<Integer> queue) {
        threadLocalRandom.ints(count).forEach(i -> queue.enqueue(value));
    }

    private void dequeueValues(int count, BlockingQueue<Integer> queue) {
        IntStream.range(0, count).forEach(i -> queue.dequeue());
    }

    private void awaitForCountDown(CountDownLatch countDownLatch) {
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
    }
}
