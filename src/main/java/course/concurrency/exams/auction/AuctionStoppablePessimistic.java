package course.concurrency.exams.auction;

import java.util.concurrent.locks.ReentrantLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Bid latestBid = INITIAL_BID;
    private volatile boolean isActive = true;

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

    @Override
    public Bid stopAuction() {
        lock.lock();
        isActive = false;
        lock.unlock();

        return latestBid;
    }

    /**
     * Checks if the new bid beats the latest bid
     * @param bid a new bid
     * @return true is the auction is active and the new bid is higher than the latest one
     */
    private boolean shouldBidBeUpdated(Bid bid) {
        return isActive && bid.getPrice() > latestBid.getPrice();
    }

}
