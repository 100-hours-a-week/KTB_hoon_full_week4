package kakao.bootcamp.fullstack.global.security.dto;

import kakao.bootcamp.fullstack.api.domain.member.Role;

public record AuthMember(Long memberId, String email, Role role) {
}