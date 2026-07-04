package kakao.bootcamp.fullstack.api.dto.request;

import kakao.bootcamp.fullstack.api.domain.member.Role;

public record AuthMember(Long memberId, String email, Role role) {
}