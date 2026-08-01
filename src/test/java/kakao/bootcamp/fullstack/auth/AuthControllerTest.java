package kakao.bootcamp.fullstack.auth;

import static kakao.bootcamp.fullstack.auth.constant.TokenExpireTestConstants.REFRESH_TOKEN_EXPIRE_SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import kakao.bootcamp.fullstack.api.controller.AuthController;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.service.AuthService;
import kakao.bootcamp.fullstack.global.constants.AuthCookieConstants;
import kakao.bootcamp.fullstack.global.config.SecurityConfig;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.rate_limiter.RateLimiter;
import kakao.bootcamp.fullstack.global.security.filter.JwtAccessDeniedHandler;
import kakao.bootcamp.fullstack.global.security.filter.JwtAuthenticationEntryPoint;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private TokenBlacklist tokenBlacklist;
    @MockitoBean private SessionBlacklist sessionBlacklist;
    @MockitoBean private RateLimiter rateLimiter;

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "Password1!";
    private static final String WRONG_PASSWORD = "wrongPassword1!";

    private static String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @Nested
    @DisplayName("POST /api/v1/login")
    class Login {

        @Test
        @DisplayName("올바른 자격증명이면 AT를 바디로, RT를 HttpOnly 쿠키(path=/api/v1/reissue)로 응답한다")
        void logsInSuccessfully() throws Exception {
            // given
            given(authService.login(new LoginReqDto(EMAIL, PASSWORD)))
                    .willReturn(
                            new LoginResult(
                                    "new-access-token",
                                    "new-refresh-token",
                                    REFRESH_TOKEN_EXPIRE_SECONDS));

            // when & then
            mockMvc.perform(
                            post("/api/v1/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginBody(EMAIL, PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("refresh_token=new-refresh-token")))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("Path=/api/v1/reissue")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

            verify(authService).login(new LoginReqDto(EMAIL, PASSWORD));
        }

        @Test
        @DisplayName("자격증명이 틀리면 401과 LOGIN_FAILED 코드를 응답한다")
        void returns401WhenLoginFails() throws Exception {
            // given
            given(authService.login(new LoginReqDto(EMAIL, WRONG_PASSWORD)))
                    .willThrow(new UnauthorizedException(AuthErrorCode.LOGIN_FAILED));

            // when & then
            mockMvc.perform(
                            post("/api/v1/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(loginBody(EMAIL, WRONG_PASSWORD)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/logout")
    class Logout {

        @Test
        @DisplayName("AT와 RT 쿠키가 있으면 둘 다 서비스로 넘기고 200을 응답한다")
        void logsOutSuccessfully() throws Exception {
            // given
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            // when & then
            mockMvc.perform(
                            post("/api/v1/logout")
                                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                    .cookie(
                                            new Cookie(
                                                    AuthCookieConstants.REFRESH_TOKEN_COOKIE,
                                                    refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("refresh_token=;")))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("Path=/api/v1/reissue")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

            verify(authService).logout(accessToken, refreshToken);
        }

        @Test
        @DisplayName("AT가 만료돼 Authorization이 없어도 RT 쿠키만으로 로그아웃되어 200을 응답한다")
        void logsOutWithRefreshTokenCookieOnly() throws Exception {
            // given
            String refreshToken = "refresh-token";

            // when & then
            mockMvc.perform(
                            post("/api/v1/logout")
                                    .cookie(
                                            new Cookie(
                                                    AuthCookieConstants.REFRESH_TOKEN_COOKIE,
                                                    refreshToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

            verify(authService).logout(null, refreshToken);
        }

        @Test
        @DisplayName("AT와 RT가 모두 없어도 예외 없이 200과 삭제 쿠키를 응답한다(멱등)")
        void logsOutIdempotentlyWithoutTokens() throws Exception {
            // when & then
            mockMvc.perform(post("/api/v1/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

            verify(authService).logout(null, null);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/reissue")
    class Reissue {

        @Test
        @DisplayName("RT 쿠키가 있으면 새 AT를 바디로, 새 RT를 HttpOnly 쿠키(path=/api/v1/reissue)로 응답한다")
        void reissuesWithCookie() throws Exception {
            // given
            given(authService.reissue("old-refresh-token"))
                    .willReturn(
                            new LoginResult(
                                    "new-access-token",
                                    "new-refresh-token",
                                    REFRESH_TOKEN_EXPIRE_SECONDS));

            // when & then
            mockMvc.perform(
                            post("/api/v1/reissue")
                                    .cookie(new Cookie("refresh_token", "old-refresh-token")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("refresh_token=new-refresh-token")))
                    .andExpect(
                            header().string(
                                            HttpHeaders.SET_COOKIE,
                                            containsString("Path=/api/v1/reissue")))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")));

            verify(authService).reissue("old-refresh-token");
        }

        @Test
        @DisplayName("RT 쿠키가 없으면 서비스에 null이 전달되고 401을 응답한다")
        void reissueWithoutCookie() throws Exception {
            // given
            given(authService.reissue(null))
                    .willThrow(new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN));

            // when & then
            mockMvc.perform(post("/api/v1/reissue"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

            verify(authService).reissue(null);
        }
    }
}
