package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostViewLog extends BaseEntity {

    private static final int VIEW_COUNT_INTERVAL_HOURS = 24;

    private Long id;
    private Long postId;
    private Long memberId;
    private LocalDateTime viewedAt;

    private PostViewLog(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
        this.viewedAt = LocalDateTime.now();
    }

    public static PostViewLog create(Long postId, Long memberId) {
        return new PostViewLog(postId, memberId);
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
        return viewedAt.plusHours(VIEW_COUNT_INTERVAL_HOURS)
                .isBefore(LocalDateTime.now());
    }

    public void refreshViewedAt() {
        this.viewedAt = LocalDateTime.now();
    }
}
