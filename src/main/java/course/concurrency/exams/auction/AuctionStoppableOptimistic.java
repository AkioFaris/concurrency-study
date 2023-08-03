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
    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(null, true);

    /**
     * Checks whether the new newBid is the highest and updates the latest newBid in this case
     *
     * @param newBid a new newBid
     * @return true if the latest newBid was updated, false otherwise
     */
    public boolean propose(Bid newBid) {
        Bid currentBid = latestBid.getReference();
        if (latestBid.isMarked() && (currentBid == null || newBid.getPrice() > currentBid.getPrice())) {
            boolean updateSucceeded = latestBid.compareAndSet(currentBid, newBid, true, true);

            while (!updateSucceeded) {
                currentBid = latestBid.getReference();
                if (!latestBid.isMarked() || newBid.getPrice() <= currentBid.getPrice()) // check if the new bid still beats the current one
                    break;
                updateSucceeded = latestBid.compareAndSet(currentBid, newBid, true, true);
            }

            if (updateSucceeded) {
                notifier.sendOutdatedMessage(currentBid);
                return true;
            }
        }
        return false;
    }

    public Bid getLatestBid(){
        return latestBid.getReference();
    }
    
    public Bid stopAuction() {
        Bid currentLatestBid = latestBid.getReference();
        latestBid.set(currentLatestBid, false);
        return currentLatestBid;
    }
}
