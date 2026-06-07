package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReport extends BaseEntity {

    private Long id;
    private Long postId;
    private Long memberId;
    private ReportReason reason;

    private PostReport(Long postId, Long memberId, ReportReason reason) {
        this.postId = postId;
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

    public static PostReport create(Long postId, Long memberId, ReportReason reason) {
        return new PostReport(postId, memberId, reason);
    }
}