package kakao.bootcamp.fullstack.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class AuthReissueConcurrencyTest {

    private static final String PASSWORD = "Password1!";

    @Autowired private AuthService authService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordHasher passwordHasher;

    @Test
    @DisplayName("같은 RT로 재발급이 동시에 들어와도 하나만 성공하고 세션은 폐기된다")
    void onlyOneReissueSucceedsForSameToken() throws Exception {
        // given
        String email = "concurrent-" + UUID.randomUUID() + "@example.com";
        Member member = MemberFixture.activeMember(email, passwordHasher.hash(PASSWORD));
        memberRepository.save(member);
        String refreshToken = authService.login(new LoginReqDto(email, PASSWORD)).refreshToken();

        AtomicInteger success = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> reissue =
                () -> {
                    ready.countDown();
                    start.await();
                    try {
                        LoginResult result = authService.reissue(refreshToken);
                        assertThat(result.refreshToken()).isNotBlank();
                        success.incrementAndGet();
                    } catch (UnauthorizedException e) {
                        rejected.incrementAndGet();
                    }
                    return null;
                };

        // when
        List<Future<Void>> futures = List.of(executor.submit(reissue), executor.submit(reissue));
        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // then
        assertThat(success.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(1);
        assertThat(refreshTokenRepository.findNotRevokedFamilyIdsByMemberId(member.getId()))
                .isEmpty();
    }
}
