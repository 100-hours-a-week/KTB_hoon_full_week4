package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import kakao.bootcamp.fullstack.api.repository.post.PostViewLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaPostViewLogRepositoryAdapter implements PostViewLogRepository {

    private final JpaPostViewLogRepository jpaPostViewLogRepository;

    @Override
    public void save(PostViewLog log) {
        jpaPostViewLogRepository.save(log);
    }

    @Override
    public Optional<PostViewLog> findByPostIdAndMemberId(Long postId, Long memberId) {
        return jpaPostViewLogRepository.findByPostIdAndMemberId(postId, memberId);
    }
}
