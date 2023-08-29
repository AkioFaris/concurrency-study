package course.concurrency.exams.auction;

public interface Auction {

    Bid INITIAL_BID = new Bid(0L, 0L, 0L);

    boolean propose(Bid bid);

    Bid getLatestBid();
}
