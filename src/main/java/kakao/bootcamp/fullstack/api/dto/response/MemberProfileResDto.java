package kakao.bootcamp.fullstack.api.dto.response;

public record MemberProfileResDto(
        String email,
        String nickname,
        String imageUrl
) {

}
