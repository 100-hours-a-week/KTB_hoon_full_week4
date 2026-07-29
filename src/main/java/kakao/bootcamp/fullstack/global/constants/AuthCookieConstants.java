package kakao.bootcamp.fullstack.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthCookieConstants {
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String REFRESH_TOKEN_PATH = "/api/v1/reissue";

    // 현재 HTTP 환경이라 미사용. HTTPS 전환 후 Secure와 함께 다시 적용한다. (SameSite=None은 Secure 전제)
    public static final String SAME_SITE = "None";
}
