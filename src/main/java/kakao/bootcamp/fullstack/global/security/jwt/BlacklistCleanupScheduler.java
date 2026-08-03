package kakao.bootcamp.fullstack.global.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "prod"})
@RequiredArgsConstructor
public class BlacklistCleanupScheduler {

    private final TokenBlacklist tokenBlacklist;
    private final SessionBlacklist sessionBlacklist;

    @Scheduled(fixedDelayString = "${security.blacklist.cleanup-interval-ms}")
    public void purgeExpired() {
        int tokens = tokenBlacklist.evictExpired();
        int sessions = sessionBlacklist.evictExpired();
        log.debug("블랙리스트 정리 실행: token={}건, session={}건", tokens, sessions);
        if (tokens > 0 || sessions > 0) {
            log.info("블랙리스트 만료 정리: token={}건, session={}건 제거", tokens, sessions);
        }
    }
}
