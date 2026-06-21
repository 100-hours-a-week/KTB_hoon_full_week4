package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    Optional<PostViewLog> findByPostIdAndMemberId(Long postId, Long memberId);
}
