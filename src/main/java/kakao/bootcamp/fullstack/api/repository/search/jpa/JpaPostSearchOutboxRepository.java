package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.search.OutboxStatus;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostSearchOutboxRepository extends JpaRepository<PostSearchOutbox, Long> {

    @Query(
            "SELECT o FROM PostSearchOutbox o WHERE o.status = :status"
                    + " AND (o.nextAttemptAt IS NULL OR o.nextAttemptAt <= :now)"
                    + " AND o.deleted = false ORDER BY o.id ASC")
    List<PostSearchOutbox> findProcessable(
            @Param("status") OutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);
}
