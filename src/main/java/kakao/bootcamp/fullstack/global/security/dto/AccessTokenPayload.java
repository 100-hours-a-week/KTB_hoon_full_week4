package kakao.bootcamp.fullstack.global.security.dto;

import kakao.bootcamp.fullstack.api.domain.member.Role;

public record AccessTokenPayload(
        Long memberId, String email, Role role, String jti, String fid, long expiresAtMillis) {

    public AuthMember toAuthMember() {
        return new AuthMember(memberId, email, role.name());
    }
}
