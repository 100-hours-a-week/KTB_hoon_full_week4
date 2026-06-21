package kakao.bootcamp.fullstack.api.repository.post.inmemory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import kakao.bootcamp.fullstack.api.repository.post.PostViewLogRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryPostViewLogRepository implements PostViewLogRepository {

    private final Map<Long, PostViewLog> logs = new ConcurrentHashMap<>();
    private final IdGenerator idGenerator = new AtomicLongIdGenerator();

    @Override
    public void save(PostViewLog log) {
        if (log.isNew()) {
            Long id = idGenerator.nextId();
            log.assignId(id);
        }
        logs.put(log.getId(), log);
    }

    @Override
    public Optional<PostViewLog> findByPostIdAndMemberId(Long postId, Long memberId) {
        return logs.values().stream()
                .filter(log -> log.getPostId().equals(postId)
                        && log.getMemberId().equals(memberId))
                .findFirst();
    }
}
