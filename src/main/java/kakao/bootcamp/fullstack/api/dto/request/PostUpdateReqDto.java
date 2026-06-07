package kakao.bootcamp.fullstack.api.dto.request;


public record PostUpdateReqDto(
        String title,
        String content,
        String imageUrl
) {}