package app.majung.server.pickup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class ApiRateLimitTest {
    @Test void writesHaveSeparateBudgetAndMinuteBoundaryResetsBoth() {
        PickupApiTest.MutableClock clock = new PickupApiTest.MutableClock();
        clock.reset();
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(clock, true, 3, 1, 10);
        assertEquals(0, limiter.acquire("a", true));
        assertEquals(429, limiter.acquire("a", true));
        assertEquals(0, limiter.acquire("a", false));
        assertEquals(0, limiter.acquire("a", false));
        assertEquals(429, limiter.acquire("a", false));
        assertEquals(0, limiter.acquire("b", true));
        clock.advance(Duration.ofMinutes(1));
        assertEquals(0, limiter.acquire("a", true));
    }

    @Test void clientMapRejectsOverflowAndReclaimsExpiredEntries() {
        PickupApiTest.MutableClock clock = new PickupApiTest.MutableClock();
        clock.reset();
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(clock, true, 2, 1, 1);
        assertEquals(0, limiter.acquire("a", false));
        assertEquals(503, limiter.acquire("b", false));
        clock.advance(Duration.ofMinutes(1));
        assertEquals(0, limiter.acquire("b", false));
    }

    @Test void concurrentCallsCannotExceedLimit() throws Exception {
        ApiRateLimitFilter limiter = new ApiRateLimitFilter(java.time.Clock.systemUTC(), true, 10, 5, 10);
        var executor = Executors.newFixedThreadPool(8);
        try {
            var calls = new ArrayList<Callable<Integer>>();
            for (int i = 0; i < 100; i++) calls.add(() -> limiter.acquire("a", false));
            long accepted = 0;
            for (var result : executor.invokeAll(calls)) if (result.get() == 0) accepted++;
            assertEquals(10, accepted);
        } finally { executor.shutdownNow(); }
    }
}
