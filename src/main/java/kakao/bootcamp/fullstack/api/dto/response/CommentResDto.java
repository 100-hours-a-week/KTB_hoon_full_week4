package kakao.bootcamp.fullstack.api.dto.response;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.post.Comment;

public record CommentResDto(
        Long commentId,
        String content,
        Long writerId,
        String writerNickname,
        boolean isMine,
        LocalDateTime createdAt
) {
    public static CommentResDto from(Comment comment, boolean isMine) {
        return new CommentResDto(
                comment.getId(),
                comment.getContent(),
                comment.getWriter().getId(),
                comment.getWriter().getNickname(),
                isMine,
                comment.getCreatedAt()
        );
    }
}