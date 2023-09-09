package course.concurrency.exams.refactoring;

import course.concurrency.exams.refactoring.Others.MountTableManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    public static final int CACHE_UPDATE_TIMEOUT_MS = 1000;
    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(CACHE_UPDATE_TIMEOUT_MS);
        routerStore = mock(Others.RouterStore.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
//        service.serviceInit(); // needed for complex class testing, not for now

        mockCachedRecords();
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

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh()).thenReturn(true))) {
            // when
            mockedService.refresh();
        }
        // then
        verifyNoExceptionLogs(mockedService);
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);

        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh()).thenReturn(false))) {
            // when
            mockedService.refresh();
        }
        // then
        verifyNoExceptionLogs(mockedService);
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(ADDRESSES.size())).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSucceededTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);

        AtomicInteger refreshCallsCount = new AtomicInteger();
        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh())
                        .then((Answer<Boolean>) invocation -> refreshCallsCount.incrementAndGet() % 2 == 1))) {
            // when
            mockedService.refresh();
        }
        // then
        verifyNoExceptionLogs(mockedService);
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(refreshCallsCount.get() / 2)).invalidate(anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    @DisplayName("One task completed with exception")
    public void someTasksEndedWithExceptions(int failedOnExceptionTasks) {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);

        AtomicInteger counter = new AtomicInteger(failedOnExceptionTasks);
        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> {
                    String address = context.arguments().get(0).toString();
                    if (counter.get() > 0) {
                        when(mock.refresh()).thenThrow(
                                new RuntimeException("Exception for " + address + " address"));
                        counter.decrementAndGet();
                    } else {
                        when(mock.refresh()).thenReturn(true);
                    }
                })) {
            // when
            mockedService.refresh();
        }
        // then
        verifyNoExceptionLogs(mockedService);
        verify(mockedService).log(String.format("Mount table entries cache refresh successCount=%d,failureCount=%d",
                ADDRESSES.size() - failedOnExceptionTasks, failedOnExceptionTasks));
        verify(routerClientsCache, times(failedOnExceptionTasks)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);

        String addressWithLongRefresh = ADDRESSES.get(0);
        try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                (mock, context) -> when(mock.refresh())
                        .then((Answer<Boolean>) invocation -> {
                            if (context.arguments().get(0).toString().contains(addressWithLongRefresh)) {
                                TimeUnit.MILLISECONDS.sleep(CACHE_UPDATE_TIMEOUT_MS + 50);
                            }
                            return true;
                        }))) {
            // when
            mockedService.refresh();
        }
        // then
        verifyNoInterruptedLog(mockedService);
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("Check log when the refresh is interrupted")
    public void refreshIsInterrupted() throws InterruptedException {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);

        Thread thread = new Thread(() -> {
            try (MockedConstruction<MountTableManager> mockedManager = mockConstruction(Others.MountTableManager.class,
                    (mock, context) -> when(mock.refresh())
                            .then((Answer<Boolean>) invocation -> {
                                TimeUnit.MILLISECONDS.sleep(CACHE_UPDATE_TIMEOUT_MS - 50);
                                return true;
                            }))) {
                // when
                mockedService.refresh();
            }
        });
        thread.start();
        thread.interrupt();
        thread.join();
        // then
        verify(mockedService).log("Mount table cache refresher was interrupted.");
        verifyNoFailedTimeoutLog(mockedService);
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(4)).invalidate(anyString());
    }

    private static void verifyNoExceptionLogs(MountTableRefresherService mockedService) {
        verifyNoInterruptedLog(mockedService);
        verifyNoFailedTimeoutLog(mockedService);
    }

    private static void verifyNoInterruptedLog(MountTableRefresherService mockedService) {
        verify(mockedService, never()).log("Mount table cache refresher was interrupted.");
    }

    private static void verifyNoFailedTimeoutLog(MountTableRefresherService mockedService) {
        verify(mockedService, never()).log("Not all router admins updated their cache");
    }

    private void mockCachedRecords() {
        List<Others.RouterState> states = ADDRESSES.stream()
                .map(Others.RouterState::new)
                .collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);
    }
}
