package kakao.bootcamp.fullstack.api.repository.search.inmemory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.search.OutboxStatus;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchOutboxRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inmemory")
public class InMemoryPostSearchOutboxRepository implements PostSearchOutboxRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, PostSearchOutbox> rows = new ConcurrentHashMap<>();

    @Override
    public void save(PostSearchOutbox outbox) {
        if (outbox.isNew()) {
            outbox.assignId(idGenerator.nextId());
        }
        rows.put(outbox.getId(), outbox);
    }

    @Override
    public List<PostSearchOutbox> findProcessable(LocalDateTime now, int limit) {
        return rows.values().stream()
                .filter(row -> !row.isDeleted())
                .filter(row -> row.getStatus() == OutboxStatus.PENDING)
                .filter(
                        row ->
                                row.getNextAttemptAt() == null
                                        || !row.getNextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(PostSearchOutbox::getId))
                .limit(limit)
                .toList();
    }
}
