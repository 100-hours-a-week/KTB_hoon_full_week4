package kakao.bootcamp.fullstack.api.dto.request;


public record PostDraftCreateReqDto(
        String title,
        String content,
        String imageUrl
) {}
