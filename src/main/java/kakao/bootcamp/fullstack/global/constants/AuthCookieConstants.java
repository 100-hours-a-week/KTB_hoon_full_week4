package kakao.bootcamp.fullstack.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthCookieConstants {
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    public static final String REFRESH_TOKEN_PATH = "/api/v1";
    public static final String SAME_SITE = "None";
}
