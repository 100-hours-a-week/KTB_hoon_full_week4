package kakao.bootcamp.fullstack.global.rate_limiter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local","prod"})
public class InMemoryPostRateLimiter implements RateLimiter {

    private final Map<Long, Window> windows = new HashMap<>();

    @Override
    public synchronized boolean tryAcquire(Long memberId, int limit, long windowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        Window window = windows.get(memberId);

        if (window == null || now.isAfter(window.expiresAt())) {
            windows.put(memberId, new Window(now.plusMinutes(windowMinutes), 1));
            return true;
        }

        if (window.count() >= limit) {
            return false;
        }

        windows.put(memberId, new Window(window.expiresAt(), window.count() + 1));
        return true;
    }

    private record Window(LocalDateTime expiresAt, int count) {}
}
