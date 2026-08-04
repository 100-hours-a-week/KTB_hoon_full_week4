package kakao.bootcamp.fullstack.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

public class CacheUsageMonitorTest {

    private static final long MAX = 100;

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;
    private CacheUsageMonitor monitor;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(CacheUsageMonitor.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        monitor = new CacheUsageMonitor("테스트", MAX);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    private List<ILoggingEvent> logs() {
        return appender.list;
    }

    @Nested
    @DisplayName("엣지 트리거")
    class EdgeTrigger {

        @Test
        @DisplayName("정상 범위에서는 로그를 남기지 않는다")
        void normalStaysSilent() {
            monitor.check(0);
            monitor.check(50);
            monitor.check(79);

            assertThat(logs()).isEmpty();
        }

        @Test
        @DisplayName("같은 상태가 반복되면 최초 전이 때 한 번만 로그를 남긴다")
        void logsOnlyOncePerState() {
            monitor.check(85); // NORMAL -> WARN (1건)
            monitor.check(86);
            monitor.check(90);

            assertThat(logs()).hasSize(1);
            assertThat(logs().get(0).getLevel()).isEqualTo(Level.WARN);
        }

        @Test
        @DisplayName("80% 도달 시 WARN, 상한 도달 시 ERROR를 남긴다")
        void warnThenFull() {
            monitor.check(80); // WARN
            monitor.check(100); // FULL

            assertThat(logs())
                    .extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.WARN, Level.ERROR);
        }

        @Test
        @DisplayName("정상으로 복귀하면 INFO를 남긴다")
        void recoversToNormal() {
            monitor.check(100); // FULL (ERROR)
            monitor.check(10); // NORMAL 복귀 (INFO)

            assertThat(logs())
                    .extracting(ILoggingEvent::getLevel)
                    .containsExactly(Level.ERROR, Level.INFO);
        }

        @Test
        @DisplayName("WARN을 건너뛰고 정상에서 곧바로 포화로 전이해도 ERROR를 남긴다")
        void jumpsStraightToFull() {
            monitor.check(100);

            assertThat(logs()).hasSize(1);
            assertThat(logs().get(0).getLevel()).isEqualTo(Level.ERROR);
        }
    }
}
