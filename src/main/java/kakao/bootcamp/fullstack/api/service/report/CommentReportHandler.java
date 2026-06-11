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
    public boolean supports(TargetType targetType) {
        return targetType == TargetType.COMMENT;
    }

    @Override
    public void handleReported(Long targetId) {
        Comment comment = commentRepository.findActiveById(targetId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));
        comment.increaseReportCount();
        commentRepository.save(comment);
    }
}
