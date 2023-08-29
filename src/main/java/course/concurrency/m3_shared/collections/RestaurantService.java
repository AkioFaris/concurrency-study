package course.concurrency.m3_shared.collections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class RestaurantService {

    private static final int PARALLELISM_THRESHOLD = Runtime.getRuntime().availableProcessors();

    private Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};

//    private final ConcurrentHashMap<String, Long> stat = new ConcurrentHashMap<>(); // solution 1
    private final ConcurrentHashMap<String, LongAdder> stat = new ConcurrentHashMap<>(); // solution 3

//    {
//        restaurantMap.keySet().forEach(key -> stat.put(key, new LongAdder())); // solution 2 (the fastest)
//    }

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        stat.putIfAbsent(restaurantName, new LongAdder());
        stat.get(restaurantName).increment();
//        stat.merge(restaurantName, 1L, (prevCount, defaultValue) -> prevCount + 1); // solution 1
    }

    public Set<String> printStat() {
        HashSet<String> statLines = new HashSet<>();
        stat.forEachEntry(PARALLELISM_THRESHOLD, record -> statLines.add(record.getKey() + " - " + record.getValue()));
        return statLines;
    }
}
