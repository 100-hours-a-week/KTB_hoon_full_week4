package kakao.bootcamp.fullstack.api.domain.post;

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
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@SQLDelete(sql = "UPDATE posts SET deleted = true, deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?")
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private long likeCount = 0L;

    @Column(nullable = false)
    private long commentCount = 0L;

    @Column(nullable = false)
    private long viewCount = 0L;

    @Column(nullable = false)
    private long reportCount = 0L;

    @Column(nullable = false)
    private boolean edited = false;

    @Column(nullable = false)
    private boolean blinded = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    private Post(Member member, String title, String content, String imageUrl) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
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

    public void increaseViewCount(){
        this.viewCount++;
    }

    public void increaseCommentCount(){
        this.commentCount++;
    }

    public long increaseLikeCount(){
        this.likeCount++;
        return likeCount;
    }

    public void increaseReportCount(){
        this.reportCount++;
        if (!blinded && reportCount >= PostConstants.BLIND_THRESHOLD) {
            this.blinded = true;
        }
    }

    public long decreaseLikeCount(){
        this.likeCount--;
        return likeCount;
    }

    public void decreaseCommentCount(){
        this.commentCount--;
    }

    public boolean isWriterWithdrawn(){
        return member.isDeleted();
    }

    public void updatePost(String title, String content, String imageUrl){
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        if(!edited) {
            this.edited = true;
        }
    }

    public boolean isWriter(Long memberId){
        return member.getId().equals(memberId);
    }

    public static Post create(Member writer, String title, String content, String imageUrl){
        return new Post(writer, title, content, imageUrl);
    }
}
