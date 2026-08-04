package kakao.bootcamp.fullstack.global.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Caffeine 로컬 캐시 기반 토큰 블랙리스트. 값(만료 epoch millis)으로 per-entry TTL을 계산해, 각 jti가 자기 만료시각에 자동 제거된다. (수동
 * 스케줄러/lazy-expiry 없이 라이브러리 TTL이 만료 정리를 담당)
 */
@Slf4j
@Component
@Profile({"local", "prod"})
public class CaffeineTokenBlacklist implements TokenBlacklist {

    private static final long MAX_SIZE = 100_000;

    private final CacheUsageMonitor usageMonitor = new CacheUsageMonitor("토큰 블랙리스트", MAX_SIZE);

    private final Cache<String, Long> blacklist =
            Caffeine.newBuilder()
                    .maximumSize(MAX_SIZE)
                    // 유휴 상태에서도 만료 시점에 곧바로 제거(→ removalListener 즉시 발화)되도록 예약
                    .scheduler(Scheduler.systemScheduler())
                    .expireAfter(
                            new Expiry<String, Long>() {
                                long ttlNanos(Long expiresAt) {
                                    long remain = expiresAt - System.currentTimeMillis();
                                    return TimeUnit.MILLISECONDS.toNanos(Math.max(0, remain));
                                }

                                @Override
                                public long expireAfterCreate(
                                        String key, Long expiresAt, long currentTime) {
                                    return ttlNanos(expiresAt);
                                }

                                @Override
                                public long expireAfterUpdate(
                                        String key,
                                        Long expiresAt,
                                        long currentTime,
                                        long currentDuration) {
                                    return ttlNanos(expiresAt);
                                }

                                @Override
                                public long expireAfterRead(
                                        String key,
                                        Long expiresAt,
                                        long currentTime,
                                        long currentDuration) {
                                    return currentDuration;
                                }
                            })
                    .removalListener(
                            (String jti, Long expiresAt, RemovalCause cause) -> {
                                if (cause == RemovalCause.EXPIRED) {
                                    log.info("토큰 블랙리스트 만료 제거: jti={}", jti);
                                }
                            })
                    .build();

    @Override
    public void add(String jti, long tokenExpiresAt) {
        blacklist.put(jti, tokenExpiresAt);
        usageMonitor.check(blacklist.estimatedSize());
    }

    @Override
    public boolean exists(String jti) {
        return blacklist.getIfPresent(jti) != null;
    }
}
