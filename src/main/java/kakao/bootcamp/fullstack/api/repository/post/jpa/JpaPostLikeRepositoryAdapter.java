package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.repository.post.PostLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

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
    public Optional<PostLike> findActiveByPostIdAndMemberId(Long postId, Long memberId) {
        return jpaPostLikeRepository.findActiveByPostIdAndMemberId(postId, memberId);
    }

    @Override
    public boolean existsByPostIdAndMemberId(Long postId, Long memberId) {
        return jpaPostLikeRepository.existsByPostIdAndMemberId(postId, memberId);
    }
}
