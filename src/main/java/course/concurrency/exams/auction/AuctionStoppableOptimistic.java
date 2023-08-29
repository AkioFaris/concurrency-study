package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    /**
     * AtomicMarkableReference of the latest bid and a state of active\closed auction
     */
    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(INITIAL_BID, true);

    /**
     * Checks whether the new newBid is the highest and updates the latest newBid in this case
     *
     * @param newBid a new newBid
     * @return true if the latest newBid was updated, false otherwise
     */
    public boolean propose(Bid newBid) {
        Bid currentBid;

        do {
            currentBid = latestBid.getReference();
            if (!latestBid.isMarked() || currentBid.getPrice() >= newBid.getPrice())
                return false;
        } while (!latestBid.compareAndSet(currentBid, newBid, true, true));

        notifier.sendOutdatedMessage(currentBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid currentLatestBid;

        do {
            currentLatestBid = latestBid.getReference();
        } while (!latestBid.attemptMark(currentLatestBid, false));

        return currentLatestBid;
    }
}
