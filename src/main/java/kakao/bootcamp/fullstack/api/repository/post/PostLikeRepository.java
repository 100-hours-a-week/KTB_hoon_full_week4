package kakao.bootcamp.fullstack.api.repository.post;

import kakao.bootcamp.fullstack.api.domain.post.PostLike;

public interface PostLikeRepository {
    void save(PostLike postLike);
    void delete(Long postId, Long memberId);
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);
}
