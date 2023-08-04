package course.concurrency.exams.auction;

import java.util.concurrent.locks.StampedLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }
    private final StampedLock stampedLock = new StampedLock();
    private volatile Bid latestBid;
    private volatile boolean isActive = true;

    /**
     * Checks whether the new bid is the highest and updates the latest bid in this case
     *
     * @param bid a new bid
     * @return true if the latest bid was updated, false otherwise
     */
    public boolean propose(Bid bid) {
        long stamp = stampedLock.tryOptimisticRead();

        if (latestBid == null || doesBeatTheLatestBid(bid)) {
            // check if the latest bid was updated by another thread or if the auction is stopped
            if (stampedLock.validate(stamp)) {
                updateLatestBid(bid);
                return true;
            } else {
                return updateLatestBidIfNeeded(bid);
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        long stamp = stampedLock.tryOptimisticRead();
        Bid lastAcceptedBid = latestBid;
        if (!stampedLock.validate(stamp)) {
            stamp = stampedLock.readLock();
            lastAcceptedBid = latestBid;
            stampedLock.unlockRead(stamp);
        }
        return lastAcceptedBid;
    }

    @Override
    public Bid stopAuction() {
        long stamp = stampedLock.writeLock();
        isActive = false;
        stampedLock.unlockWrite(stamp);

        return latestBid;
    }

    /**
     * Checks if the new bid beats the latest bid
     * @param bid a new bid
     * @return true is the auction is active and the new bid is higher than the latest one
     */
    private boolean doesBeatTheLatestBid(Bid bid) {
        return isActive && bid.getPrice() > latestBid.getPrice();
    }

    private void updateLatestBid(Bid bid) {
        long stamp = stampedLock.writeLock();
        notifier.sendOutdatedMessage(latestBid);
        latestBid = bid;
        stampedLock.unlockWrite(stamp);
    }

    private boolean updateLatestBidIfNeeded(Bid bid) {
        boolean isBidUpdated = false;
        long stamp = stampedLock.writeLock();
        if (doesBeatTheLatestBid(bid)) {
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            isBidUpdated = true;
        }
        stampedLock.unlockWrite(stamp);
        return isBidUpdated;
    }
}
