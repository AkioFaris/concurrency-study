package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10L, 45L, 66L, 345L, 234L, 333L, 67L, 123L, 768L);

    private static final int MAX_SHOP_RESPONSE_TIME = 2950;

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        ExecutorService executorService = Executors.newCachedThreadPool();

        List<CompletableFuture<Double>> pricesCfs = shopIds.stream()
                .map(shopId -> CompletableFuture
                        .supplyAsync(() -> priceRetriever.getPrice(itemId, shopId), executorService)
                        .exceptionally(throwable -> Double.NaN)
                        .completeOnTimeout(Double.NaN, MAX_SHOP_RESPONSE_TIME, TimeUnit.MILLISECONDS))
                .collect(Collectors.toList());
        CompletableFuture.allOf(pricesCfs.toArray(CompletableFuture[]::new)).join();

        return pricesCfs.stream()
                .map(this::getPrice)
                .min(Double::compareTo)
                .orElse(Double.NaN);
    }

    private Double getPrice(CompletableFuture<Double> priceCf) {
        try {
            return priceCf.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to get price from CompletableFuture: " + e);
        }
    }
}
