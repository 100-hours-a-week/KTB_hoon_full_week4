package kakao.bootcamp.fullstack.auth;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TokenExpireTestConstants {
    public static final long ACCESS_TOKEN_EXPIRE_SECONDS = 600L;
    public static final long REFRESH_TOKEN_EXPIRE_SECONDS = 1_209_600L;
}
