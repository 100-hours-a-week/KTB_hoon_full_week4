package kakao.bootcamp.fullstack.global.security.jwt.provider;

import kakao.bootcamp.fullstack.api.domain.member.Role;

public interface JwtProvider {
    String createAccessToken(Long memberId, String email, Role role, String familyId);

    void validateToken(String token);

    Long getMemberId(String token);

    String getEmail(String token);

    String getJti(String token);

    String getFid(String token);

    Role getRole(String token);

    long getExpirationMillis(String token);
}
