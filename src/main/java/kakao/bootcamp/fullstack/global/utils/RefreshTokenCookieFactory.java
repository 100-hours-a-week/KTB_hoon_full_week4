package kakao.bootcamp.fullstack.global.utils;

import java.time.Duration;
import kakao.bootcamp.fullstack.global.constants.AuthCookieConstants;
import org.springframework.http.ResponseCookie;

public class RefreshTokenCookieFactory {

    public static ResponseCookie create(String refreshToken, long maxAgeSeconds) {
        // TODO : HTTPS 적용 시 secure 옵션 키기
        return ResponseCookie.from(AuthCookieConstants.REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(AuthCookieConstants.REFRESH_TOKEN_PATH)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    public static ResponseCookie delete(){
        return ResponseCookie.from(AuthCookieConstants.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path(AuthCookieConstants.REFRESH_TOKEN_PATH)
                .maxAge(0)
                .build();
    }
}
