package kakao.bootcamp.fullstack.global.security.jwt;

import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

/**
 * 로컬 캐시 사용량이 상한(maximumSize)에 근접·도달하는지 관측한다.
 *
 * <p>상태 전이(정상↔경고↔포화) 시에만 로그를 남기는 엣지 트리거 방식이라, 포화 상황에서 매 연산마다 로그가 쏟아지는 것을 막는다. 80% 이상이면 경고, 상한 도달이면
 * 에러를 남기고, 다시 내려오면 복귀를 알린다.
 */
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
