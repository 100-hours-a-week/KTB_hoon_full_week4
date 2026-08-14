package kakao.bootcamp.fullstack.global.security.jwt;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "prod", "test"})
public class CaffeineSessionBlacklist implements SessionBlacklist {

    private static final long MAX_SIZE = 100_000;

    private final CacheUsageMonitor usageMonitor;

    private final Cache<String, Long> blacklist;

    public CaffeineSessionBlacklist() {
        this(Ticker.systemTicker(), MAX_SIZE);
    }

    CaffeineSessionBlacklist(Ticker ticker, long maxSize) {
        this.usageMonitor = new CacheUsageMonitor("세션 블랙리스트", maxSize);
        this.blacklist =
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .ticker(ticker)
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
                                (String familyId, Long expiresAt, RemovalCause cause) -> {
                                    if (cause == RemovalCause.EXPIRED) {
                                        log.info("세션 블랙리스트 만료 제거: familyId={}", familyId);
                                    }
                                })
                        .build();
    }

    @Override
    public void add(String familyId, long tokenExpiresAt) {
        blacklist.put(familyId, tokenExpiresAt);
        usageMonitor.check(blacklist.estimatedSize());
    }

    @Override
    public boolean exists(String familyId) {
        return blacklist.getIfPresent(familyId) != null;
    }
}
