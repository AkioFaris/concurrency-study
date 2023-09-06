package course.concurrency.exams.refactoring;

import course.concurrency.exams.refactoring.Others.MountTableManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    public static final List<String> ADDRESSES = List.of("123", "local6", "789", "local");
    public static final int CACHE_UPDATE_TIMEOUT = 1000;
    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(CACHE_UPDATE_TIMEOUT);
        routerStore = mock(Others.RouterStore.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
//        service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
//        service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        mockCachedRecords();

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh()).thenReturn(true))) {
            // when
            mockedService.refresh();
        }
        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        mockCachedRecords();

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh()).thenReturn(false))) {
            // when
            mockedService.refresh();
        }
        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(ADDRESSES.size())).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSucceededTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        mockCachedRecords();
        AtomicInteger refreshCallsCount = new AtomicInteger();

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh())
                        .then((Answer<Boolean>) invocation -> refreshCallsCount.incrementAndGet() % 2 == 1))) {
            // when
            mockedService.refresh();
        }
        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(refreshCallsCount.get() / 2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        mockCachedRecords();
        String addressWithFailedRefresh = ADDRESSES.get(0);

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> {
                    if (context.arguments().get(0).toString().contains(addressWithFailedRefresh)) {
                        when(mock.refresh()).thenThrow(
                                new RuntimeException("Exception for " + addressWithFailedRefresh + " address"));
                    } else {
                        when(mock.refresh()).thenReturn(true);
                    }
                })) {
            // when
            mockedService.refresh();
        }
        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        mockCachedRecords();
        String addressWithLongRefresh = ADDRESSES.get(0);

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh())
                        .then((Answer<Boolean>) invocation -> {
                            if (context.arguments().get(0).toString().contains(addressWithLongRefresh)) {
                                TimeUnit.MILLISECONDS.sleep(CACHE_UPDATE_TIMEOUT + 50);
                            }
                            return true;
                        }))) {
            // when
            mockedService.refresh();
        }
        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    private void mockCachedRecords() {
        List<Others.RouterState> states = ADDRESSES.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
    }
}
