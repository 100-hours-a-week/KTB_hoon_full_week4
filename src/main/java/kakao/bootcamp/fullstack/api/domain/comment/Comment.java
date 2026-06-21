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
import kakao.bootcamp.fullstack.api.domain.post.Post;
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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private long reportCount = 0L;

    @Column(nullable = false)
    private boolean blinded = false;

    // optional = false : 연관된 엔티티가 반드시 존재해야 한다는 설정
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    private Comment(Post post, Member member, String content) {
        this.post = post;
        this.member = member;
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
        return member.getId().equals(memberId);
    }

    public boolean isWriterWithdrawn(){
        return member.isDeleted();
    }

    public static Comment create(Post post, Member writer, String content) {
        return new Comment(post, writer, content);
    }
}
