package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReportHandler implements ReportTargetHandler {

    private final PostRepository postRepository;

    @Override
    public boolean supports(TargetType targetType) {
        return targetType == TargetType.POST;
    }

    @Override
    public void handleReported(Long targetId) {
        Post post = postRepository.findActiveById(targetId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
        post.increaseReportCount();
        postRepository.save(post);
    }
}
