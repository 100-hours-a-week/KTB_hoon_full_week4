package kakao.bootcamp.fullstack.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtConstants {
    public static final String TOKEN_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
}