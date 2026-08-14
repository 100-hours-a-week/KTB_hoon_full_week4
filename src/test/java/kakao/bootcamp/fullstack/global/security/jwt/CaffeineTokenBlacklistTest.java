package kakao.bootcamp.fullstack.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Duration;
import java.util.List;
import kakao.bootcamp.fullstack.global.security.jwt.fake.FakeTicker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class CaffeineTokenBlacklistTest {

    private static final String JTI = "jti-1";
    private static final Duration TTL = Duration.ofSeconds(600);
    private static final long MAX_SIZE = 10;

    private FakeTicker ticker;
    private CaffeineTokenBlacklist tokenBlacklist;
    private ListAppender<ILoggingEvent> appender;
    private Logger monitorLogger;

    @BeforeEach
    void setUp() {
        ticker = new FakeTicker();
        tokenBlacklist = new CaffeineTokenBlacklist(ticker, MAX_SIZE);
        monitorLogger = (Logger) LoggerFactory.getLogger(CacheUsageMonitor.class);
        appender = new ListAppender<>();
        appender.start();
        monitorLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        monitorLogger.detachAppender(appender);
    }

    private List<String> monitorLogs() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
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
    @DisplayName("TTL로 엔트리가 사라지면 사용량 경보가 정상으로 복귀한다")
    void usageReturnsToNormalAfterExpiry() {
        // given
        for (int i = 0; i < MAX_SIZE; i++) {
            tokenBlacklist.add("jti-" + i, expiresAfter(TTL));
        }
        assertThat(monitorLogs()).anyMatch(message -> message.contains("포화"));

        // when
        ticker.advance(TTL.plusSeconds(1));
        for (int i = 0; i < MAX_SIZE; i++) {
            tokenBlacklist.exists("jti-" + i);
        }

        // then
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                assertThat(monitorLogs())
                                        .anyMatch(message -> message.contains("정상 복귀")));
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
