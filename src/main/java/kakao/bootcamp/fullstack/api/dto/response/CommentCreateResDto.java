package kakao.bootcamp.fullstack.api.dto.response;

public record CommentCreateResDto(Long commentId) {

    public static CommentCreateResDto from(Long commentId) {
        return new CommentCreateResDto(commentId);
    }
}
