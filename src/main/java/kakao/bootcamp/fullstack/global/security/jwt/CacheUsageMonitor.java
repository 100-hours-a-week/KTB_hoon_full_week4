package kakao.bootcamp.fullstack.global.security.jwt;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CacheUsageMonitor {

    private static final int NORMAL = 0;
    private static final int WARN = 1;
    private static final int FULL = 2;

    private final String name;
    private final long maxSize;
    private final long warnSize;
    private final AtomicInteger level = new AtomicInteger(NORMAL);

    public CacheUsageMonitor(String name, long maxSize) {
        this.name = name;
        this.maxSize = maxSize;
        this.warnSize = (long) (maxSize * 0.8);
    }

    /** 현재 엔트리 수를 받아 상태 전이가 있을 때만 로그를 남긴다. */
    public void check(long currentSize) {
        int now = currentSize >= maxSize ? FULL : currentSize >= warnSize ? WARN : NORMAL;
        if (level.getAndSet(now) == now) {
            return;
        }
        switch (now) {
            case FULL -> log.error("{} 포화: {}/{} — 유효 엔트리 축출 위험", name, currentSize, maxSize);
            case WARN -> log.warn("{} 80% 도달: {}/{}", name, currentSize, maxSize);
            default -> log.info("{} 사용량 정상 복귀: {}/{}", name, currentSize, maxSize);
        }
    }
}
