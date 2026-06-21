package kakao.bootcamp.fullstack.api.repository.post.jpa;

import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPostLikeRepository extends JpaRepository<PostLike, Long> {

    void deleteByPost_IdAndMember_Id(Long postId, Long memberId);

    boolean existsByPost_IdAndMember_Id(Long postId, Long memberId);
}
