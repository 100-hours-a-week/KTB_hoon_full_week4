package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Comment;
import kakao.bootcamp.fullstack.api.domain.post.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Report;
import kakao.bootcamp.fullstack.api.domain.post.ReportErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.TargetType;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.repository.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.PostRepository;
import kakao.bootcamp.fullstack.api.repository.ReportRepository;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public void report(Long memberId, PostReportReqDto request) {
        checkMemberExists(memberId);
        validateTargetExists(request.targetId(), request.targetType());
        checkNotAlreadyReported(request.targetId(), request.targetType(), memberId);
        Report report = Report.create(request.targetId(), request.targetType(), memberId, request.reportReason());
        reportRepository.save(report);
        // TODO : targetType이 늘어날때마다 같이 늘어나는 분기문. 어떻게 하지...
        if (request.targetType().equals(TargetType.POST)) {
            Post post = loadPostOrThrow(request.targetId());
            post.increaseReportCount();
            postRepository.save(post);
        }
    }

    private Post loadPostOrThrow(Long request) {
        return postRepository.findById(request)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }

    private void checkMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException(AuthErrorCode.MEMBER_NOT_FOUND);
        }
    }

    private void validateTargetExists(Long targetId, TargetType targetType) {
        switch (targetType) {
            case POST -> loadPostOrThrow(targetId);
            case COMMENT -> loadCommentOrThrow(targetId);
        }
    }

    private Comment loadCommentOrThrow(Long targetId) {
        return commentRepository.findById(targetId)
                .orElseThrow(() -> new NotFoundException(CommentErrorCode.COMMENT_NOT_FOUND));
    }

    private void checkNotAlreadyReported(Long targetId, TargetType targetType, Long memberId) {
        if (reportRepository.existsByTargetAndMember(targetId, targetType, memberId)) {
            throw new ConflictException(ReportErrorCode.ALREADY_REPORTED);
        }
    }
}
