package kakao.bootcamp.fullstack.post.fake;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import kakao.bootcamp.fullstack.api.repository.post.PostViewLogRepository;

public class FakePostViewLogRepository implements PostViewLogRepository {

    private final Map<Long, PostViewLog> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(PostViewLog log) {
        if (log.isNew()) {
            log.assignId(sequence.incrementAndGet());
        }
        store.put(log.getId(), log);
    }

    @Override
    public Optional<PostViewLog> findByPostIdAndMemberId(Long postId, Long memberId) {
        return store.values().stream()
                .filter(log -> Objects.equals(log.getPostId(), postId))
                .filter(log -> Objects.equals(log.getMemberId(), memberId))
                .findFirst();
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
