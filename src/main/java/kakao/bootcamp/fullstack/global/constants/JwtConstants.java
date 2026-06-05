package kakao.bootcamp.fullstack.global.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtConstants {
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String LOGIN_MEMBER_ID_ATTRIBUTE = "LOGIN_MEMBER_ID";
    public static final String ACCESS_TOKEN_ATTRIBUTE = "ACCESS_TOKEN";
}