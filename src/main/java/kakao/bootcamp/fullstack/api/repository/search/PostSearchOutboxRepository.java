package kakao.bootcamp.fullstack.api.repository.search;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;

public interface PostSearchOutboxRepository {

    void save(PostSearchOutbox outbox);

    List<PostSearchOutbox> findProcessable(LocalDateTime now, int limit);
}
