package kakao.bootcamp.fullstack.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.auth.fake.FakeJwtProvider;
import kakao.bootcamp.fullstack.auth.fake.FakeSessionBlacklist;
import kakao.bootcamp.fullstack.auth.fake.FakeTokenBlacklist;
import kakao.bootcamp.fullstack.global.constants.JwtConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

public class JwtAuthenticationFilterTest {

    private static final Long MEMBER_ID = 1L;
    private static final String EMAIL = "test@example.com";
    private static final String FAMILY_ID = "family-1";
    private static final String PROTECTED_URI = "/api/v1/profile";

    private FakeJwtProvider jwtProvider;
    private FakeTokenBlacklist tokenBlacklist;
    private FakeSessionBlacklist sessionBlacklist;
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        jwtProvider = new FakeJwtProvider();
        tokenBlacklist = new FakeTokenBlacklist();
        sessionBlacklist = new FakeSessionBlacklist();
        filter = new JwtAuthenticationFilter(jwtProvider, tokenBlacklist, sessionBlacklist);

        request = new MockHttpServletRequest();
        request.setRequestURI(PROTECTED_URI);
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private String issueTokenAndSetHeader() {
        String token = jwtProvider.createAccessToken(MEMBER_ID, EMAIL, Role.ROLE_USER, FAMILY_ID);
        request.addHeader(JwtConstants.TOKEN_HEADER, JwtConstants.BEARER_PREFIX + token);
        return token;
    }

    @Test
    @DisplayName("한 요청에서 토큰을 한 번만 파싱한다")
    void parsesTokenOncePerRequest() throws Exception {
        // given
        issueTokenAndSetHeader();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(jwtProvider.parseCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("유효한 토큰이면 SecurityContext에 인증 회원을 담고 다음 필터로 넘긴다")
    void authenticatesWithValidToken() throws Exception {
        // given
        issueTokenAndSetHeader();

        // when
        filter.doFilter(request, response, filterChain);

        // then
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal).isEqualTo(new AuthMember(MEMBER_ID, EMAIL, Role.ROLE_USER.name()));
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("토큰이 없으면 인증 없이 다음 필터로 넘긴다")
    void passesThroughWithoutToken() throws Exception {
        // when
        filter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(filterChain.getRequest()).isNotNull();
        assertThat(jwtProvider.parseCount()).isZero();
    }

    @Test
    @DisplayName("블랙리스트에 등록된 jti면 INVALID_TOKEN을 던진다")
    void rejectsBlacklistedToken() {
        // given
        String token = issueTokenAndSetHeader();
        tokenBlacklist.add(jwtProvider.getJti(token), System.currentTimeMillis() + 600_000);

        // when & then
        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .extracting(BusinessException::getCode)
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("폐기된 세션의 fid면 INVALID_TOKEN을 던진다")
    void rejectsRevokedSession() {
        // given
        issueTokenAndSetHeader();
        sessionBlacklist.add(FAMILY_ID, System.currentTimeMillis() + 600_000);

        // when & then
        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(() -> filter.doFilter(request, response, filterChain))
                .extracting(BusinessException::getCode)
                .isEqualTo(AuthErrorCode.INVALID_TOKEN);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
