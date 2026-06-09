package kakao.bootcamp.fullstack.global.jwt.provider;

public interface JwtProvider {
    String createAccessToken(Long memberId, String email);
    void validateToken(String token);
    Long getMemberId(String token);
    String getEmail(String token);
    String getJti(String token);
    long getExpirationMillis(String token);
}
