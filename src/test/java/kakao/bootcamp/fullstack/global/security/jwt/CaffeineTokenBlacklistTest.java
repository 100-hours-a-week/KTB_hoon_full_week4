package kakao.bootcamp.fullstack.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import kakao.bootcamp.fullstack.global.security.jwt.fake.FakeTicker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CaffeineTokenBlacklistTest {

    private static final String JTI = "jti-1";
    private static final Duration TTL = Duration.ofSeconds(600);
    private static final long MAX_SIZE = 10;

    private FakeTicker ticker;
    private CaffeineTokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        ticker = new FakeTicker();
        tokenBlacklist = new CaffeineTokenBlacklist(ticker, MAX_SIZE);
    }

    private long expiresAfter(Duration duration) {
        return System.currentTimeMillis() + duration.toMillis();
    }

    @Test
    @DisplayName("등록하지 않은 jti는 존재하지 않는다")
    void doesNotExistWhenNotAdded() {
        assertThat(tokenBlacklist.exists(JTI)).isFalse();
    }

    @Test
    @DisplayName("등록한 jti는 AT 만료 전까지 존재한다")
    void existsBeforeExpiry() {
        // given
        tokenBlacklist.add(JTI, expiresAfter(TTL));

        // when
        ticker.advance(TTL.minusSeconds(1));

        // then
        assertThat(tokenBlacklist.exists(JTI)).isTrue();
    }

    @Test
    @DisplayName("AT 만료 시각이 지나면 존재하지 않는다")
    void doesNotExistAfterExpiry() {
        // given
        tokenBlacklist.add(JTI, expiresAfter(TTL));

        // when
        ticker.advance(TTL.plusSeconds(1));

        // then
        assertThat(tokenBlacklist.exists(JTI)).isFalse();
    }

    @Test
    @DisplayName("이미 만료된 AT의 jti를 등록하면 곧바로 존재하지 않는다")
    void doesNotExistWhenAlreadyExpired() {
        // when
        tokenBlacklist.add(JTI, expiresAfter(Duration.ofSeconds(-1)));

        // then
        assertThat(tokenBlacklist.exists(JTI)).isFalse();
    }

    @Test
    @DisplayName("조회해도 만료 시각이 연장되지 않는다")
    void readDoesNotExtendExpiry() {
        // given
        tokenBlacklist.add(JTI, expiresAfter(TTL));
        ticker.advance(TTL.dividedBy(2));
        assertThat(tokenBlacklist.exists(JTI)).isTrue();

        // when
        ticker.advance(TTL.dividedBy(2).plusSeconds(1));

        // then
        assertThat(tokenBlacklist.exists(JTI)).isFalse();
    }

    @Test
    @DisplayName("만료 시각이 다른 jti는 각자의 시각에 만료된다")
    void expiryIsPerEntry() {
        // given
        tokenBlacklist.add("jti-short", expiresAfter(Duration.ofSeconds(60)));
        tokenBlacklist.add("jti-long", expiresAfter(TTL));

        // when
        ticker.advance(Duration.ofSeconds(61));

        // then
        assertThat(tokenBlacklist.exists("jti-short")).isFalse();
        assertThat(tokenBlacklist.exists("jti-long")).isTrue();
    }
}
