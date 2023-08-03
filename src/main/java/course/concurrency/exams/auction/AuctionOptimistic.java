package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    /**
     * Checks whether the new newBid is the highest and updates the latest newBid in this case
     *
     * @param newBid a new newBid
     * @return true if the latest newBid was updated, false otherwise
     */
    public boolean propose(Bid newBid) {
        Bid currentBid = latestBid.get();
        if (currentBid == null || newBid.getPrice() > currentBid.getPrice()) {
            boolean updateSucceeded = latestBid.compareAndSet(currentBid, newBid);

            while (!updateSucceeded) {
                currentBid = latestBid.get();
                if (newBid.getPrice() <= currentBid.getPrice()) // check if the new bid still beats the current one
                    break;
                updateSucceeded = latestBid.compareAndSet(currentBid, newBid);
            }

            if (updateSucceeded) {
                notifier.sendOutdatedMessage(currentBid);
                return true;
            }
        }
        return false;
    }

    public Bid getLatestBid(){
        return latestBid.get();
    }

}
