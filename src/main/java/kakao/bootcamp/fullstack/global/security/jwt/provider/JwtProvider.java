package kakao.bootcamp.fullstack.global.security.jwt.provider;

import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.security.dto.AccessTokenPayload;

public interface JwtProvider {
    String createAccessToken(Long memberId, String email, Role role, String familyId);

    AccessTokenPayload parseAccessToken(String token);
}
