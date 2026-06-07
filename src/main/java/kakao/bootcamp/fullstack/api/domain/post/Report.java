package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseEntity {

    private Long id;
    private Long targetId;
    private TargetType targetType;
    private Long memberId;
    private ReportReason reason;

    private Report(Long targetId, TargetType targetType, Long memberId, ReportReason reason) {
        this.targetId = targetId;
        this.targetType = targetType;
        this.memberId = memberId;
        this.reason = reason;
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new IllegalStateException("이미 ID가 할당된 신고 기록입니다.");
        }
        this.id = id;
    }

    public static Report create(Long targetId, TargetType targetType, Long memberId, ReportReason reason) {
        return new Report(targetId, targetType, memberId, reason);
    }
}
