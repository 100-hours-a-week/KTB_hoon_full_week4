package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.search.OutboxStatus;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaPostSearchOutboxRepositoryAdapter implements PostSearchOutboxRepository {

    private final JpaPostSearchOutboxRepository jpaPostSearchOutboxRepository;

    @Override
    public void save(PostSearchOutbox outbox) {
        jpaPostSearchOutboxRepository.save(outbox);
    }

    @Override
    public List<PostSearchOutbox> findProcessable(LocalDateTime now, int limit) {
        return jpaPostSearchOutboxRepository.findProcessable(
                OutboxStatus.PENDING, now, PageRequest.of(0, limit));
    }
}
