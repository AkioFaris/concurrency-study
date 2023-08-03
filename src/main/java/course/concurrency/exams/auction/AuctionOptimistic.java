package course.concurrency.exams.auction;

import java.util.concurrent.locks.StampedLock;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final StampedLock stampedLock = new StampedLock();

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid;

    /**
     * Checks whether the new bid is the highest and updates the latest bid in this case
     *
     * @param bid a new bid
     * @return true if the latest bid was updated, false otherwise
     */
    public boolean propose(Bid bid) {
        long stamp = stampedLock.tryOptimisticRead();

        if (latestBid == null || isNewBidHigher(bid)) {
            if (stampedLock.validate(stamp)) { // checking if the latest bid was updated by another thread
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

    private boolean isNewBidHigher(Bid bid) {
        return bid.getPrice() > latestBid.getPrice();
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
        if (isNewBidHigher(bid)) {
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            isBidUpdated = true;
        }
        stampedLock.unlockWrite(stamp);
        return isBidUpdated;
    }
}
