package course.concurrency.exams.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = r -> {
            Thread t = new Thread();
            t.setName("MountTableRefresh_ClientsCacheCleaner");
            t.setDaemon(true);
            return t;
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        List<MountTableRefresher> refreshers = new ArrayList<>();
        for (Others.RouterState routerState : cachedRecords) {
            String adminAddress = routerState.getAdminAddress();
            if (adminAddress == null || adminAddress.length() == 0) {
                // this router has not enabled router admin.
                continue;
            }
            if (isLocalAdmin(adminAddress)) {
                /*
                 * Local router's cache update does not require RPC call, so no need for
                 * RouterClient
                 */
                refreshers.add(getLocalRefresher(adminAddress));
            } else {
                refreshers.add(new MountTableRefresher(
                        new Others.MountTableManager(adminAddress), adminAddress));
            }
        }
        if (!refreshers.isEmpty()) {
            invokeRefresh(refreshers);
        }
    }

    protected MountTableRefresher getLocalRefresher(String adminAddress) {
        return new MountTableRefresher(new Others.MountTableManager("local"), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresher> refreshers) {
        ConcurrentSkipListSet<String> failedAddresses = refreshers.stream()
                .map(MountTableRefresher::getAdminAddress)
                .collect(Collectors.toCollection(ConcurrentSkipListSet::new));

        // start all the threads
        CompletableFuture<?>[] completableFutures = refreshers.stream()
                .map(refresher -> CompletableFuture.supplyAsync(refresher::refresh)
                        .orTimeout(cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                        .whenCompleteAsync((res, ex) -> updateRefreshFailedAddresses(refresher, failedAddresses, res)))
                .toArray(CompletableFuture<?>[]::new);

        // Wait for all the threads to complete within the cache update timeout
        try {
            CompletableFuture.allOf(completableFutures).get();
        } catch (InterruptedException e) {
            log("Mount table cache refresher was interrupted.");
        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log("Not all router admins updated their cache");
            }
        }
        // for each failed refresh remove RouterClient from cache so that new client is created
        failedAddresses.forEach(this::removeFromCache);

        logResult(refreshers.size(), failedAddresses.size());
    }

    private void logResult(int allTasksCount, int failureCount) {
        int successCount = allTasksCount - failureCount;
        log(String.format("Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    private void updateRefreshFailedAddresses(MountTableRefresher refresher, ConcurrentSkipListSet<String> failedAddresses,
                                              Boolean result) {
        if (Boolean.TRUE.equals(result)) {
            failedAddresses.remove(refresher.getAdminAddress());
        }
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache<String, Others.RouterClient> cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}