package course.concurrency.m3_shared.deadlock;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

public class ConcurrentHashMapDeadLockTest {

    @AllArgsConstructor
    public static class CustomKey {

        private int key;

        /**
         * With this hash code we will have only two buckets in any hash map
         *
         * @return hash code
         */
        @Override
        public int hashCode() {
            return key % 2;
        }
    }


    @Test
    public void deadlockTest() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        ConcurrentHashMap<CustomKey, Integer> map = new ConcurrentHashMap<>();

        int poolSize = 2;
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) {
            int finalI = i;
            executor.submit(() -> {
                awaitForTheLatch(latch);

                map.compute(new CustomKey(finalI), (customKey, integer) ->
                        map.put(new CustomKey(finalI + 1), finalI));
                // or here instead of map.put() can be map.clear() or any other table update operation
                // that will try to obtain a lock, occupied by another thread (for example, T2).
                // To achieve a deadlock
                // the second thread T2 should wait to lock the bucket already locked by the current thread
            });
        }
        latch.countDown();

        executor.shutdown();
        int terminatedTimeoutMin = 5;
        boolean terminated = executor.awaitTermination(terminatedTimeoutMin, TimeUnit.MINUTES);
        System.out.println("Map: " + map.entrySet());
        System.out.println("Is terminated within " + terminatedTimeoutMin + " minutes: " + terminated);
    }

    private void awaitForTheLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ignored) {
        }
    }
}
