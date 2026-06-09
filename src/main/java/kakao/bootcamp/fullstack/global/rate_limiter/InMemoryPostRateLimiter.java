package kakao.bootcamp.fullstack.global.rate_limiter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class InMemoryPostRateLimiter implements RateLimiter {

    private static final long WINDOW_MINUTES = 1; // fixed window 방식 사용
    private static final int LIMIT = 3;

    private final Map<Long, Window> windows = new HashMap<>();

    @Override
    public synchronized boolean tryAcquire(Long memberId) {
        LocalDateTime now = LocalDateTime.now();
        Window window = windows.get(memberId);

        if (window == null || now.isAfter(window.expiresAt())) {
            windows.put(memberId, new Window(now.plusMinutes(WINDOW_MINUTES), 1));
            return true;
        }

        if (window.count() >= LIMIT) {
            return false;
        }

        windows.put(memberId, new Window(window.expiresAt(), window.count() + 1));
        return true;
    }

    private record Window(LocalDateTime expiresAt, int count) {}
}
