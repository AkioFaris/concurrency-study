package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notifier {

    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    public void sendOutdatedMessage(Bid bid) {
        if (bid.equals(Auction.INITIAL_BID)) {
            return;
        }
        executorService.submit(this::imitateSending);
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
