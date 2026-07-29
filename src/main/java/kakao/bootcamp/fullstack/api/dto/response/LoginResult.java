package kakao.bootcamp.fullstack.api.dto.response;

public record LoginResult(
        String accessToken, String refreshToken, long refreshTokenMaxAgeSeconds) {}
