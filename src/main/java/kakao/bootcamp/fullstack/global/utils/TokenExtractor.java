package kakao.bootcamp.fullstack.global.utils;


public class TokenExtractor {
    public static final String BEARER_PREFIX = "Bearer ";

    public static String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authHeader.substring(7).trim();
    }
}
