package kakao.bootcamp.fullstack.search.fake;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.search.OutboxStatus;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchOutboxRepository;

public class FakePostSearchOutboxRepository implements PostSearchOutboxRepository {

    private final Map<Long, PostSearchOutbox> rows = new LinkedHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(PostSearchOutbox outbox) {
        if (outbox.isNew()) {
            outbox.assignId(sequence.incrementAndGet());
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
                .limit(limit)
                .toList();
    }

    public List<PostSearchOutbox> rows() {
        return List.copyOf(rows.values());
    }

    public List<Long> savedPostIds() {
        return rows.values().stream().map(PostSearchOutbox::getPostId).toList();
    }

    public void clear() {
        rows.clear();
        sequence.set(0);
    }
}
