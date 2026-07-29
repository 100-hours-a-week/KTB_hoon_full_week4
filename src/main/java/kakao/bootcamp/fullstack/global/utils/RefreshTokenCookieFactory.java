package kakao.bootcamp.fullstack.global.utils;

import java.time.Duration;
import kakao.bootcamp.fullstack.global.constants.AuthCookieConstants;
import org.springframework.http.ResponseCookie;

public class RefreshTokenCookieFactory {

    public static ResponseCookie create(String refreshToken, long maxAgeSeconds) {
        // 현재 서비스가 HTTP로 동작하므로 Secure/SameSite 옵션은 적용하지 않는다.
        // Secure 쿠키는 HTTPS에서만 저장/전송되고, SameSite=None은 Secure를 전제로 하기 때문.
        // HTTPS 전환 시 .secure(true) + .sameSite(AuthCookieConstants.SAME_SITE)를 다시 활성화한다.
        return ResponseCookie.from(AuthCookieConstants.REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .path(AuthCookieConstants.REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }
}
