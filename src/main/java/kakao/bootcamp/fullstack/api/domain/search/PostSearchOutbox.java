package kakao.bootcamp.fullstack.api.domain.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

// 색인 동기화 요청. "무엇을 하라"가 아니라 post_id 만 담는다 — 폴러가 처리 시점의
// 게시글 상태를 다시 읽어 반영하므로 요청 간 순서가 꼬여도 최종 상태로 수렴한다.
@Getter
@Entity
@SQLDelete(
        sql =
                "UPDATE post_search_outbox SET deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(
        name = "post_search_outbox",
        indexes = {
            @Index(
                    name = "idx_post_search_outbox_status_next_attempt_at",
                    columnList = "status, next_attempt_at")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostSearchOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status = OutboxStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "next_attempt_at")
    private LocalDateTime nextAttemptAt;

    private PostSearchOutbox(Long postId) {
        this.postId = postId;
        this.status = OutboxStatus.PENDING;
    }

    public static PostSearchOutbox create(Long postId) {
        return new PostSearchOutbox(postId);
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new BusinessException(CommonErrorCode.ALREADY_ASSIGNED_ID);
        }
        this.id = id;
    }

    public void markDone() {
        this.status = OutboxStatus.DONE;
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }

    public void retryLater(LocalDateTime nextAttemptAt) {
        this.retryCount++;
        this.nextAttemptAt = nextAttemptAt;
    }
}
