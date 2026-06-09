package kakao.bootcamp.fullstack.global.utils;


import kakao.bootcamp.fullstack.global.constants.JwtConstants;

public class TokenExtractor {

    public static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(JwtConstants.BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(7).trim();
    }
}
