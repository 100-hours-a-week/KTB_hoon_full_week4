package kakao.bootcamp.fullstack.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.UUID;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.global.security.token.RefreshTokenHasher;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class AuthReissueTransactionTest {

    @Autowired private AuthService authService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PasswordHasher passwordHasher;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private RefreshTokenHasher refreshTokenHasher;

    @Test
    @DisplayName("재사용 감지로 예외가 던져져도 family 무효화(revoke)는 커밋되어 롤백되지 않는다")
    void familyRevocationIsCommittedDespiteException() {
        // given
        String unique = UUID.randomUUID().toString();
        String email = "something-" + unique + "@example.com";
        String password = "Password1!";
        memberRepository.save(MemberFixture.activeMember(email, passwordHasher.hash(password)));

        LoginResult login = authService.login(new LoginReqDto(email, password));
        String rt1 = login.refreshToken();
        LoginResult rotated = authService.reissue(rt1);
        String rt2 = rotated.refreshToken();
        assertThat(storedTokenOf(rt2).isRevoked()).isFalse();

        // when
        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> authService.reissue(rt1))
                .extracting(BusinessException::getCode)
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);

        // then
        assertThat(storedTokenOf(rt2).isRevoked()).isTrue();
    }

    private RefreshToken storedTokenOf(String rawRefreshToken) {
        return refreshTokenRepository
                .findNotDeletedByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow();
    }
}
