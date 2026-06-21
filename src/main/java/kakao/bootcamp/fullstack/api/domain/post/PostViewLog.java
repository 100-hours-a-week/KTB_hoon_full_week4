package kakao.bootcamp.fullstack.api.domain.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

// TODO : 추후 리팩토링 검토
//  - (postId, memberId) 복합키(@EmbeddedId)로 전환하여 대체 키 제거
//  - 단순 조회 기록용이므로 BaseEntity 상속 및 soft delete 적용이 적합한지 재검토
@Getter
@Entity
@Table(name = "post_view_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private LocalDateTime viewedAt;

    private PostViewLog(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.viewedAt = LocalDateTime.now();
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

    public boolean canCountAsNewView() {
        return viewedAt
                .plusHours(PostConstants.VIEW_COUNT_INTERVAL_HOURS)
                .isBefore(LocalDateTime.now());
    }

    public void refreshViewedAt() {
        this.viewedAt = LocalDateTime.now();
    }

    public static PostViewLog create(Long postId, Long memberId) {
        return new PostViewLog(postId, memberId);
    }
}
