package kakao.bootcamp.fullstack.api.repository.post;

import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaPostLikeRepositoryAdapter implements PostLikeRepository {

    private final JpaPostLikeRepository jpaPostLikeRepository;

    @Override
    public void save(PostLike postLike) {
        jpaPostLikeRepository.save(postLike);
    }

    @Override
    public void delete(Long postId, Long memberId) {
        jpaPostLikeRepository.deleteByPost_IdAndMember_Id(postId, memberId);
    }

    @Override
    public boolean existsByPostIdAndMemberId(Long postId, Long memberId) {
        return jpaPostLikeRepository.existsByPost_IdAndMember_Id(postId, memberId);
    }
}
