package course.concurrency.m6;

import course.concurrency.m6.queue.priority.AgeDependentEntry;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.time.temporal.ChronoUnit.DAYS;

public class AgeDependentPriorityBlockingQueueTest {

    @Test
    public void testElementsOrderBeforeAgeThreshold() {
        PriorityBlockingQueue<AgeDependentEntry<Integer>> queue = new PriorityBlockingQueue<>();

        addElementsToQueue(queue);

        List<Integer> actualValues = drainAndUnwrap(queue);

        List<Integer> naturalSortedValues = new ArrayList<>(actualValues);
        naturalSortedValues.sort(Integer::compareTo);

        Assertions.assertThat(actualValues).isEqualTo(naturalSortedValues);
        System.out.println("Queue elements BEFORE age timeout: " + actualValues);
    }

    @Test
    public void testElementsOrderAfterAgeThreshold() throws InterruptedException {
        PriorityBlockingQueue<AgeDependentEntry<Integer>> queue = new PriorityBlockingQueue<>();

        addElementsToQueue(queue);

        TimeUnit.SECONDS.sleep(7);
        List<Integer> actualValues = drainAndUnwrap(queue);

        List<Integer> naturalSortedValues = new ArrayList<>(actualValues);
        naturalSortedValues.sort(Integer::compareTo);

        Assertions.assertThat(actualValues).isNotEqualTo(naturalSortedValues);
        System.out.println("Queue elements AFTER age timeout: " + actualValues);
    }

    private void addElementsToQueue(PriorityBlockingQueue<AgeDependentEntry<Integer>> queue) {
        for(int i = 10; i < 20; ++i) {
            queue.add(new AgeDependentEntry<>(i, Duration.of(5, ChronoUnit.SECONDS)));
        }
        // queue = { 10 11 12 13 14 15 16 17 18 19 20 }
        for(int i = 0; i < 10; ++i) {
            queue.add(new AgeDependentEntry<>(i, Duration.of(5, DAYS)));
        }
        // queue = { 0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 }
    }

    private List<Integer> drainAndUnwrap(PriorityBlockingQueue<AgeDependentEntry<Integer>> queue) {
        List<AgeDependentEntry<Integer>> elements = new ArrayList<>();
        queue.drainTo(elements);

        return elements.stream()
                .map(AgeDependentEntry::getEntry)
                .collect(Collectors.toList());
    }
}
