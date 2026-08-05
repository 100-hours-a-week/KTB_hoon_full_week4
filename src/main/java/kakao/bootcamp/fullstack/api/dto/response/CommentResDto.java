package kakao.bootcamp.fullstack.api.dto.response;

import static kakao.bootcamp.fullstack.global.constants.CommentConstants.BLINDED_COMMENT;
import static kakao.bootcamp.fullstack.global.constants.PostConstants.UNKNOWN_WRITER;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;

public record CommentResDto(
        Long commentId,
        String content,
        Long memberId,
        String writerNickname,
        boolean isMine,
        boolean isBlind,
        LocalDateTime createdAt) {
    public static CommentResDto from(Comment comment, boolean isMine) {
        boolean blinded = comment.isBlinded();
        return new CommentResDto(
                comment.getId(),
                blinded ? BLINDED_COMMENT : comment.getContent(),
                comment.getMember().getId(),
                comment.isWriterWithdrawn()
                        ? UNKNOWN_WRITER
                        : comment.getMember()
                                .getNickname(), // UNKNOWN_WRITER가 현재 PostConstant에서 가져옴
                isMine,
                blinded,
                comment.getCreatedAt());
    }
}
