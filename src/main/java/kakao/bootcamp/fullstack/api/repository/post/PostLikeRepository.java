package kakao.bootcamp.fullstack.api.repository.post;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;

public interface PostLikeRepository {
    void save(PostLike postLike);
    void delete(Long postId, Long memberId);
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
    long countByPostId(Long postId);
    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long memberId);
    List<PostLike> findAllByPostId(Long postId);
    List<PostLike> findAllByMemberId(Long memberId);
}
