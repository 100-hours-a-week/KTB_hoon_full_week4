package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostReportHandler implements ReportTargetHandler {

    private final PostRepository postRepository;
    private final PostSearchIndex postSearchIndex;

    @Override
    public TargetType getTargetType() {
        return TargetType.POST;
    }

    @Override
    public boolean isWrittenBy(Long targetId, Long memberId) {
        return loadPostOrThrow(targetId).isWriter(memberId);
    }

    @Override
    public void handleReported(Long targetId) {
        Post post = loadPostOrThrow(targetId);
        post.increaseReportCount();
        postRepository.save(post);
        if (post.isBlinded()) {
            postSearchIndex.index(post);
        }
    }

    private Post loadPostOrThrow(Long targetId) {
        return postRepository
                .findActiveById(targetId)
                .orElseThrow(() -> new NotFoundException(PostErrorCode.POST_NOT_FOUND));
    }
}
