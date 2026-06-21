package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;

public interface PostLikeRepository {
    void save(PostLike postLike);
    Optional<PostLike> findActiveByPostIdAndMemberId(Long postId, Long memberId);
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
