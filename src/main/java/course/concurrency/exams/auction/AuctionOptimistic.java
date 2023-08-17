package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>(INITIAL_BID);

    /**
     * Checks whether the new newBid is the highest and updates the latest newBid in this case
     *
     * @param newBid a new newBid
     * @return true if the latest newBid was updated, false otherwise
     */
    public boolean propose(Bid newBid) {
        Bid currentBid;

        do {
            currentBid = latestBid.get();
            if (currentBid.getPrice() > newBid.getPrice())
                return false;
        } while (!latestBid.compareAndSet(currentBid, newBid));

        notifier.sendOutdatedMessage(currentBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }

}
