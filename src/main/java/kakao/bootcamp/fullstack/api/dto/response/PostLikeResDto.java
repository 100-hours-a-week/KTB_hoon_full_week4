package kakao.bootcamp.fullstack.api.dto.response;

public record PostLikeResDto(
        Long likeCount,
        boolean isLikedByMe
) {
    public static PostLikeResDto from(Long likeCount, boolean isLikedByMe) {
        return new PostLikeResDto(likeCount, isLikedByMe);
    }
}