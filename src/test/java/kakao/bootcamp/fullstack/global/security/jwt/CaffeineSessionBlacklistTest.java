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

public class CaffeineSessionBlacklistTest {

    private static final String FAMILY_ID = "family-1";
    private static final Duration TTL = Duration.ofSeconds(600);
    private static final long MAX_SIZE = 10;

    private FakeTicker ticker;
    private CaffeineSessionBlacklist sessionBlacklist;
    private ListAppender<ILoggingEvent> appender;
    private Logger monitorLogger;

    @BeforeEach
    void setUp() {
        ticker = new FakeTicker();
        sessionBlacklist = new CaffeineSessionBlacklist(ticker, MAX_SIZE);
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
    @DisplayName("등록하지 않은 familyId는 존재하지 않는다")
    void doesNotExistWhenNotAdded() {
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isFalse();
    }

    @Test
    @DisplayName("등록한 familyId는 만료 전까지 존재한다")
    void existsBeforeExpiry() {
        // given
        sessionBlacklist.add(FAMILY_ID, expiresAfter(TTL));

        // when
        ticker.advance(TTL.minusSeconds(1));

        // then
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isTrue();
    }

    @Test
    @DisplayName("만료 시각이 지나면 존재하지 않는다")
    void doesNotExistAfterExpiry() {
        // given
        sessionBlacklist.add(FAMILY_ID, expiresAfter(TTL));

        // when
        ticker.advance(TTL.plusSeconds(1));

        // then
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isFalse();
    }

    @Test
    @DisplayName("이미 지난 만료 시각으로 등록하면 곧바로 존재하지 않는다")
    void doesNotExistWhenAlreadyExpired() {
        // when
        sessionBlacklist.add(FAMILY_ID, expiresAfter(Duration.ofSeconds(-1)));

        // then
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isFalse();
    }

    @Test
    @DisplayName("조회해도 만료 시각이 연장되지 않는다")
    void readDoesNotExtendExpiry() {
        // given
        sessionBlacklist.add(FAMILY_ID, expiresAfter(TTL));
        ticker.advance(TTL.dividedBy(2));
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isTrue();

        // when
        ticker.advance(TTL.dividedBy(2).plusSeconds(1));

        // then
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isFalse();
    }

    @Test
    @DisplayName("같은 familyId를 더 늦은 만료 시각으로 다시 등록하면 만료가 갱신된다")
    void reAddExtendsExpiry() {
        // given
        sessionBlacklist.add(FAMILY_ID, expiresAfter(TTL));
        ticker.advance(TTL.minusSeconds(1));

        // when
        sessionBlacklist.add(FAMILY_ID, expiresAfter(TTL));
        ticker.advance(TTL.minusSeconds(1));

        // then
        assertThat(sessionBlacklist.exists(FAMILY_ID)).isTrue();
    }

    @Test
    @DisplayName("TTL로 엔트리가 사라지면 사용량 경보가 정상으로 복귀한다")
    void usageReturnsToNormalAfterExpiry() {
        // given
        for (int i = 0; i < MAX_SIZE; i++) {
            sessionBlacklist.add("family-" + i, expiresAfter(TTL));
        }
        assertThat(monitorLogs()).anyMatch(message -> message.contains("포화"));

        // when
        ticker.advance(TTL.plusSeconds(1));
        for (int i = 0; i < MAX_SIZE; i++) {
            sessionBlacklist.exists("family-" + i);
        }

        // then
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(
                        () ->
                                assertThat(monitorLogs())
                                        .anyMatch(message -> message.contains("정상 복귀")));
    }

    @Test
    @DisplayName("한 familyId가 만료돼도 다른 familyId는 남는다")
    void expiryIsPerEntry() {
        // given
        sessionBlacklist.add("family-short", expiresAfter(Duration.ofSeconds(60)));
        sessionBlacklist.add("family-long", expiresAfter(TTL));

        // when
        ticker.advance(Duration.ofSeconds(61));

        // then
        assertThat(sessionBlacklist.exists("family-short")).isFalse();
        assertThat(sessionBlacklist.exists("family-long")).isTrue();
    }
}
