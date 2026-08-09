package kakao.bootcamp.fullstack.global.rate_limiter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "prod", "test"})
@RequiredArgsConstructor
public class InMemoryPostRateLimiter implements RateLimiter {

    private final Clock clock;

    private final Map<Long, Deque<Instant>> requestLogs = new HashMap<>();

    @Override
    public synchronized boolean tryAcquire(Long memberId, int limit, long windowMinutes) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(Duration.ofMinutes(windowMinutes));

        Deque<Instant> log = requestLogs.computeIfAbsent(memberId, key -> new ArrayDeque<>());

        while (!log.isEmpty() && log.peekFirst().isBefore(windowStart)) {
            log.pollFirst();
        }

        if (log.size() >= limit) {
            return false;
        }

        log.addLast(now);
        return true;
    }
}
