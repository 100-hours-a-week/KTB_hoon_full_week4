package kakao.bootcamp.fullstack.global.rate_limiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "prod", "test"})
public class InMemoryPostRateLimiter implements RateLimiter {

    private static final long MAX_SIZE = 100_000;

    private final Clock clock;
    private final Cache<Long, Window> requestLogs;

    public InMemoryPostRateLimiter(Clock clock) {
        this.clock = clock;
        this.requestLogs =
                Caffeine.newBuilder()
                        .maximumSize(MAX_SIZE)
                        .ticker(() -> TimeUnit.MILLISECONDS.toNanos(clock.millis()))
                        .scheduler(Scheduler.systemScheduler())
                        .expireAfter(
                                new Expiry<Long, Window>() {
                                    long ttlNanos(Window window) {
                                        return TimeUnit.MINUTES.toNanos(window.windowMinutes());
                                    }

                                    @Override
                                    public long expireAfterCreate(
                                            Long key, Window window, long currentTime) {
                                        return ttlNanos(window);
                                    }

                                    @Override
                                    public long expireAfterUpdate(
                                            Long key,
                                            Window window,
                                            long currentTime,
                                            long currentDuration) {
                                        return ttlNanos(window);
                                    }

                                    @Override
                                    public long expireAfterRead(
                                            Long key,
                                            Window window,
                                            long currentTime,
                                            long currentDuration) {
                                        return currentDuration;
                                    }
                                })
                        .build();
    }

    @Override
    public boolean tryAcquire(Long memberId, int limit, long windowMinutes) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(Duration.ofMinutes(windowMinutes));
        boolean[] acquired = new boolean[1];

        requestLogs
                .asMap()
                .compute(
                        memberId,
                        (key, window) -> {
                            Deque<Instant> hits =
                                    window == null ? new ArrayDeque<>() : window.hits();
                            while (!hits.isEmpty() && hits.peekFirst().isBefore(windowStart)) {
                                hits.pollFirst();
                            }
                            acquired[0] = hits.size() < limit;
                            if (acquired[0]) {
                                hits.addLast(now);
                            }
                            return new Window(hits, windowMinutes);
                        });

        return acquired[0];
    }

    long trackedMembers() {
        requestLogs.cleanUp();
        return requestLogs.estimatedSize();
    }

    private record Window(Deque<Instant> hits, long windowMinutes) {}
}
