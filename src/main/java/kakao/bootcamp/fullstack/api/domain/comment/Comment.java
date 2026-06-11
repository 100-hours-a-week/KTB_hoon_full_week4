package kakao.bootcamp.fullstack.api.domain.comment;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.constants.CommentConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    private Long id;
    private Long postId;
    private Member writer;
    private String content;
    private boolean edited;
    private long reportCount = 0L;
    private boolean blinded = false;

    private Comment(Long postId, Member writer, String content) {
        this.postId = postId;
        this.writer = writer;
        this.content = content;
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

    public void updateContent(String content) {
        this.content = content;
        if (!edited) {
            this.edited = true;
        }
    }

    public void increaseReportCount() {
        this.reportCount++;
        if (!blinded && reportCount >= CommentConstants.BLIND_THRESHOLD) {
            this.blinded = true;
        }
    }

    public boolean isWriter(Long memberId) {
        return writer.getId().equals(memberId);
    }

    public static Comment create(Long postId, Member writer, String content) {
        return new Comment(postId, writer, content);
    }
}
