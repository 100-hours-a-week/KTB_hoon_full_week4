package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostViewLogRepository extends JpaRepository<PostViewLog, Long> {

    @Query("SELECT pvl FROM PostViewLog pvl WHERE pvl.memberId = :memberId AND pvl.postId = :postId")
    Optional<PostViewLog> findByPostIdAndMemberId(@Param("postId") Long postId, @Param("memberId")Long memberId);
}
