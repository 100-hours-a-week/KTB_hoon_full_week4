package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.comment.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReportHandler implements ReportTargetHandler {

    private final CommentRepository commentRepository;

    @Override
    public TargetType getTargetType() {
        return TargetType.COMMENT;
    }

    @Override
    public boolean isWrittenBy(Long targetId, Long memberId) {
        return loadCommentOrThrow(targetId).isWriter(memberId);
    }

    @Override
    public void handleReported(Long targetId) {
        Comment comment = loadCommentOrThrow(targetId);
        comment.increaseReportCount();
        commentRepository.save(comment);
    }

    private Comment loadCommentOrThrow(Long targetId) {
        return commentRepository
                .findActiveById(targetId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));
    }
}
