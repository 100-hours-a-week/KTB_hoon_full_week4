package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;

public interface PostViewLogRepository {
    void save(PostViewLog log);
    Optional<PostViewLog> findByPostIdAndMemberId(Long postId, Long memberId);
}
