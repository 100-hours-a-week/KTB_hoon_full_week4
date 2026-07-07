package kakao.bootcamp.fullstack.global.security.dto;

public record AuthMember(Long memberId, String email, String role) {
}