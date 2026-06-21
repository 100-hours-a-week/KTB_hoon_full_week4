package kakao.bootcamp.fullstack.api.domain.comment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.constants.CommentConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private long reportCount = 0L;

    @Column(nullable = false)
    private boolean blinded = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member writer;

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

    public boolean isWriterWithdrawn(){
        return writer.isDeleted();
    }

    public static Comment create(Long postId, Member writer, String content) {
        return new Comment(postId, writer, content);
    }
}
