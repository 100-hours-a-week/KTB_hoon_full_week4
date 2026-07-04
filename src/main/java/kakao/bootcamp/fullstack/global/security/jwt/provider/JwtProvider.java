package kakao.bootcamp.fullstack.global.security.jwt.provider;

import kakao.bootcamp.fullstack.api.domain.member.Role;

public interface JwtProvider {
    String createAccessToken(Long memberId, String email, Role role);
    void validateToken(String token);
    Long getMemberId(String token);
    String getEmail(String token);
    String getJti(String token);
    Role getRole(String token);
    long getExpirationMillis(String token);
}
