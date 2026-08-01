package kakao.bootcamp.fullstack.auth;

import static kakao.bootcamp.fullstack.auth.constant.TokenExpireTestConstants.ACCESS_TOKEN_EXPIRE_SECONDS;
import static kakao.bootcamp.fullstack.auth.constant.TokenExpireTestConstants.REFRESH_TOKEN_EXPIRE_SECONDS;
import static kakao.bootcamp.fullstack.auth.fixture.RefreshTokenFixture.active;
import static kakao.bootcamp.fullstack.auth.fixture.RefreshTokenFixture.expired;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.auth.fake.FakeJwtProvider;
import kakao.bootcamp.fullstack.auth.fake.FakePasswordHasher;
import kakao.bootcamp.fullstack.auth.fake.FakeRefreshTokenRepository;
import kakao.bootcamp.fullstack.auth.fake.FakeSessionBlacklist;
import kakao.bootcamp.fullstack.auth.fake.FakeTokenBlacklist;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.jwt.properties.JwtProperties;
import kakao.bootcamp.fullstack.global.security.token.RefreshTokenGenerator;
import kakao.bootcamp.fullstack.global.security.token.RefreshTokenHasher;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AuthServiceTest {

    private static final String RAW_PASSWORD = "Password1!";

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private FakePasswordHasher passwordHasher;
    private FakeJwtProvider jwtProvider;
    private RefreshTokenGenerator refreshTokenGenerator;
    private RefreshTokenHasher refreshTokenHasher;
    private FakeTokenBlacklist tokenBlacklist;
    private FakeSessionBlacklist sessionBlacklist;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        passwordHasher = new FakePasswordHasher();
        jwtProvider = new FakeJwtProvider();
        refreshTokenGenerator = new RefreshTokenGenerator();
        refreshTokenHasher = new RefreshTokenHasher();
        tokenBlacklist = new FakeTokenBlacklist();
        sessionBlacklist = new FakeSessionBlacklist();
        JwtProperties jwtProperties =
                new JwtProperties(
                        "test-secret", ACCESS_TOKEN_EXPIRE_SECONDS, REFRESH_TOKEN_EXPIRE_SECONDS);

        authService =
                new AuthService(
                        memberRepository,
                        refreshTokenRepository,
                        passwordHasher,
                        jwtProvider,
                        refreshTokenGenerator,
                        refreshTokenHasher,
                        tokenBlacklist,
                        sessionBlacklist,
                        jwtProperties);
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("정상적으로 로그인하면 AT와 RT를 발급하고 RT를 저장한다")
        void loginsSuccessfully() {
            // given
            Member member = registerMember("user@example.com");

            // when
            LoginResult result =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));

            // then
            assertThat(result.accessToken()).isNotBlank();
            assertThat(result.refreshToken()).isNotBlank();
            assertThat(result.refreshTokenMaxAgeSeconds()).isEqualTo(REFRESH_TOKEN_EXPIRE_SECONDS);

            RefreshToken saved = storedTokenOf(result.refreshToken());
            assertThat(saved.getMemberId()).isEqualTo(member.getId());
            assertThat(saved.isRevoked()).isFalse();
            assertThat(saved.getFamilyId()).isNotBlank();
            assertThat(jwtProvider.getFid(result.accessToken())).isEqualTo(saved.getFamilyId());
        }

        @Test
        @DisplayName("비밀번호가 일치하지 않으면 LOGIN_FAILED 예외를 던진다")
        void throwsWhenPasswordMismatch() {
            // given
            registerMember("user@example.com");

            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(
                            () ->
                                    authService.login(
                                            new LoginReqDto("user@example.com", "wrongPassword1!")))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.LOGIN_FAILED);
        }

        @Test
        @DisplayName("존재하지 않는 이메일이면 LOGIN_FAILED 예외를 던진다")
        void throwsWhenEmailNotFound() {
            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(
                            () ->
                                    authService.login(
                                            new LoginReqDto("nobody@example.com", RAW_PASSWORD)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.LOGIN_FAILED);
        }
    }

    @Nested
    @DisplayName("reissue()")
    class Reissue {

        @Test
        @DisplayName("유효한 RT면 기존 RT를 폐기하고 같은 family로 새 RT와 AT를 발급한다")
        void rotatesSuccessfully() {
            // given
            registerMember("user@example.com");
            LoginResult login =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));
            String oldRefreshToken = login.refreshToken();
            String familyId = storedTokenOf(oldRefreshToken).getFamilyId();

            // when
            LoginResult result = authService.reissue(oldRefreshToken);

            // then
            assertThat(result.refreshToken()).isNotEqualTo(oldRefreshToken);
            assertThat(result.accessToken()).isNotBlank();

            RefreshToken old = storedTokenOf(oldRefreshToken);
            assertThat(old.isRevoked()).isTrue();

            RefreshToken rotated = storedTokenOf(result.refreshToken());
            assertThat(rotated.isRevoked()).isFalse();
            assertThat(rotated.getFamilyId()).isEqualTo(familyId);
            assertThat(jwtProvider.getFid(result.accessToken())).isEqualTo(familyId);
        }

        @Test
        @DisplayName("이미 폐기된 RT가 다시 제출되면 재사용을 감지해 family 전체를 무효화하고 세션을 블랙리스트에 등록한다")
        void detectsReuse() {
            // given
            registerMember("user@example.com");
            LoginResult login =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));
            String rt1 = login.refreshToken();
            String familyId = storedTokenOf(rt1).getFamilyId();
            LoginResult reissued = authService.reissue(rt1);
            String rt2 = reissued.refreshToken();

            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(rt1))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);

            assertThat(storedTokenOf(rt2).isRevoked()).isTrue();
            assertThat(sessionBlacklist.exists(familyId)).isTrue();
        }

        @Test
        @DisplayName("만료된 RT면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenExpired() {
            // given
            String rawToken = refreshTokenGenerator.generate();
            RefreshToken expiredToken = expired("family-a", refreshTokenHasher.hash(rawToken));
            refreshTokenRepository.save(expiredToken);

            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(rawToken))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("RT가 null이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenNull() {
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(null))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("RT가 빈 문자열이면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenBlank() {
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue("   "))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("저장되지 않은 RT면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenNotFound() {
            // given
            String unknownToken = refreshTokenGenerator.generate();

            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(unknownToken))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("RT의 회원이 존재하지 않으면 INVALID_REFRESH_TOKEN 예외를 던진다")
        void throwsWhenMemberNotFound() {
            // given
            String rawToken = refreshTokenGenerator.generate();
            RefreshToken orphan = active("family-a", refreshTokenHasher.hash(rawToken));
            refreshTokenRepository.save(orphan);

            // when & then
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(rawToken))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("AT의 jti와 RT의 세션(family)을 블랙리스트에 등록하고 family를 폐기한다")
        void blacklistsJtiAndSession() {
            // given
            registerMember("user@example.com");
            LoginResult login =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));
            String accessToken = login.accessToken();
            String refreshToken = login.refreshToken();
            String familyId = storedTokenOf(refreshToken).getFamilyId();

            // when
            authService.logout(accessToken, refreshToken);

            // then
            assertThat(tokenBlacklist.exists(jwtProvider.getJti(accessToken))).isTrue();
            assertThat(sessionBlacklist.exists(familyId)).isTrue();
            assertThat(storedTokenOf(refreshToken).isRevoked()).isTrue();
        }

        @Test
        @DisplayName("AT가 null이어도 RT 쿠키만으로 family를 폐기하고 세션을 블랙리스트에 등록한다")
        void logsOutWithRefreshTokenOnly() {
            // given
            registerMember("user@example.com");
            LoginResult login =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));
            String refreshToken = login.refreshToken();
            String familyId = storedTokenOf(refreshToken).getFamilyId();

            // when
            authService.logout(null, refreshToken);

            // then
            assertThat(sessionBlacklist.exists(familyId)).isTrue();
            assertThat(storedTokenOf(refreshToken).isRevoked()).isTrue();
        }

        @Test
        @DisplayName("AT와 RT가 모두 없어도 예외 없이 멱등하게 처리한다")
        void logsOutIdempotentlyWithoutTokens() {
            // when & then
            authService.logout(null, null);
        }

        @Test
        @DisplayName("저장되지 않은 RT면 예외 없이 아무 것도 폐기하지 않는다")
        void ignoresUnknownRefreshToken() {
            // given
            String unknownToken = refreshTokenGenerator.generate();

            // when & then
            authService.logout(null, unknownToken);
        }
    }

    @Nested
    @DisplayName("재사용 감지 시 방어 조치 상세")
    class ReuseDetectionEffects {

        @Test
        @DisplayName("재사용 감지 시 예외를 던지기 전에 family revoke와 세션 블랙리스트 등록을 수행한다")
        void appliesDefensiveWritesBeforeThrowing() {
            // given
            registerMember("user@example.com");
            LoginResult login =
                    authService.login(new LoginReqDto("user@example.com", RAW_PASSWORD));
            String rt1 = login.refreshToken();
            String familyId = storedTokenOf(rt1).getFamilyId();
            authService.reissue(rt1);

            // when
            assertThatExceptionOfType(UnauthorizedException.class)
                    .isThrownBy(() -> authService.reissue(rt1));

            // then
            Optional<RefreshToken> active =
                    refreshTokenRepository.findActiveByTokenHash(refreshTokenHasher.hash(rt1));
            assertThat(active).isPresent();
            assertThat(active.get().isRevoked()).isTrue();
            assertThat(sessionBlacklist.exists(familyId)).isTrue();
        }
    }

    private Member registerMember(String email) {
        Member member = MemberFixture.activeMember(email, RAW_PASSWORD);
        memberRepository.save(member);
        return member;
    }

    private RefreshToken storedTokenOf(String rawRefreshToken) {
        return refreshTokenRepository
                .findActiveByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow();
    }
}
