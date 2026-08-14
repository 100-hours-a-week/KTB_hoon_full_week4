package kakao.bootcamp.fullstack.global.rate_limiter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class InMemoryPostRateLimiterTest {

    private static final int LIMIT = 3;
    private static final long WINDOW_MINUTES = 1;
    private static final Long MEMBER_ID = 1L;

    private MutableClock clock;
    private InMemoryPostRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        rateLimiter = new InMemoryPostRateLimiter(clock);
    }

    @Nested
    @DisplayName("기본 동작")
    class BasicBehavior {

        @Test
        @DisplayName("윈도우 내 limit건까지는 허용하고 그 다음 요청은 거부한다")
        void allowsUpToLimitThenRejects() {
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();

            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();
        }

        @Test
        @DisplayName("서로 다른 회원은 독립적으로 카운트된다")
        void countsIndependentlyPerMember() {
            Long otherMemberId = 2L;

            for (int i = 0; i < LIMIT; i++) {
                assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            }
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();

            assertThat(rateLimiter.tryAcquire(otherMemberId, LIMIT, WINDOW_MINUTES)).isTrue();
        }
    }

    @Nested
    @DisplayName("슬라이딩 윈도우 특성")
    class SlidingBehavior {

        @Test
        @DisplayName("가장 오래된 요청만 윈도우를 벗어나면 그만큼만 다시 허용된다")
        void allowsExactlyAsOldestRequestExpires() {
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES); // t=0s
            clock.advance(Duration.ofSeconds(1));
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES); // t=1s
            clock.advance(Duration.ofSeconds(1));
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES); // t=2s

            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();

            clock.advance(Duration.ofSeconds(59)); // t=61s, t=0s 요청만 윈도우(60s) 밖으로 벗어남
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();

            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();
        }

        @Test
        @DisplayName("윈도우 내 모든 요청이 오래되면 다시 처음부터 limit건 허용된다")
        void resetsFullyOnceAllEntriesExpire() {
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);

            clock.advance(Duration.ofMinutes(2));

            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();
        }
    }

    @Nested
    @DisplayName("엔트리 수명")
    class EntryLifetime {

        @Test
        @DisplayName("윈도우가 지나도록 요청이 없으면 회원 엔트리가 제거된다")
        void evictsIdleMemberEntry() {
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);
            assertThat(rateLimiter.trackedMembers()).isEqualTo(1);

            clock.advance(Duration.ofMinutes(WINDOW_MINUTES).plusSeconds(1));

            assertThat(rateLimiter.trackedMembers()).isZero();
        }

        @Test
        @DisplayName("윈도우 안에서 다시 요청하면 엔트리가 유지된다")
        void keepsEntryWhileActive() {
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);

            clock.advance(Duration.ofSeconds(30));
            rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);
            clock.advance(Duration.ofSeconds(30));

            assertThat(rateLimiter.trackedMembers()).isEqualTo(1);
        }

        @Test
        @DisplayName("엔트리가 제거돼도 허용 횟수는 처음부터 다시 센다")
        void evictionDoesNotChangeBehavior() {
            for (int i = 0; i < LIMIT; i++) {
                rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES);
            }
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isFalse();

            clock.advance(Duration.ofMinutes(WINDOW_MINUTES).plusSeconds(1));

            assertThat(rateLimiter.trackedMembers()).isZero();
            assertThat(rateLimiter.tryAcquire(MEMBER_ID, LIMIT, WINDOW_MINUTES)).isTrue();
        }
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.systemDefault();
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
