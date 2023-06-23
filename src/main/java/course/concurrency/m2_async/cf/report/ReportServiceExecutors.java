package course.concurrency.m2_async.cf.report;

import course.concurrency.m2_async.cf.LoadGenerator;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

public class ReportServiceExecutors {

    //    private ExecutorService executor = Executors.newSingleThreadExecutor();
//    private ExecutorService executor = Executors.newCachedThreadPool();
//    private ExecutorService executor = Executors.newFixedThreadPool(4); // N\2
//    private ExecutorService executor = Executors.newFixedThreadPool(8); // N = CPU cores
//    private ExecutorService executor = Executors.newFixedThreadPool(16); // L_N = logical cores
//    private ExecutorService executor = Executors.newFixedThreadPool(24); // N*3
//    private ExecutorService executor = Executors.newFixedThreadPool(32); // L_N*2
//    private ExecutorService executor = Executors.newFixedThreadPool(48); // L_N*3
//    private ExecutorService executor = Executors.newFixedThreadPool(150);
    private ExecutorService executor = new ThreadPoolExecutor(16, 16,
            100, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(100));


    private LoadGenerator loadGenerator = new LoadGenerator();

    public Others.Report getReport() {
        try {
            Future<Collection<Others.Item>> iFuture =
                    executor.submit(() -> getItems());
            Future<Collection<Others.Customer>> customersFuture =
                    executor.submit(() -> getActiveCustomers());
            try {
                Collection<Others.Customer> customers = customersFuture.get();
                Collection<Others.Item> items = iFuture.get();
                return combineResults(items, customers);
            } catch (ExecutionException | InterruptedException ignored) {
            }
        } catch (RejectedExecutionException rejectedExecutionException) {
            System.out.println("getReport() failed with: " + rejectedExecutionException);
        }

        return new Others.Report();
    }

    private Others.Report combineResults(Collection<Others.Item> items, Collection<Others.Customer> customers) {
        return new Others.Report(true);
    }

    private Collection<Others.Customer> getActiveCustomers() {
        loadGenerator.work();
        loadGenerator.work();
        return List.of(new Others.Customer(), new Others.Customer());
    }

    private Collection<Others.Item> getItems() {
        loadGenerator.work();
        return List.of(new Others.Item(), new Others.Item());
    }

    public void shutdown() {
        executor.shutdown();
    }
}
