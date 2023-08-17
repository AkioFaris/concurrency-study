package course.concurrency.exams.auction;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final ReentrantLock lock = new ReentrantLock();
    private volatile Bid latestBid = INITIAL_BID;

    /**
     * Checks whether the new bid is the highest and updates the latest bid in this case
     *
     * @param bid a new bid
     * @return true if the latest bid was updated, false otherwise
     */
    public boolean propose(Bid bid) {
        if (shouldBidBeUpdated(bid)) {
            try {
                lock.lock();
                if (shouldBidBeUpdated(bid)) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            } finally {
                lock.unlock();
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

    private boolean shouldBidBeUpdated(Bid bid) {
        return bid.getPrice() > latestBid.getPrice();
    }
}
