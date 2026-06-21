package kakao.bootcamp.fullstack.api.domain.edit_revision;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "edit_revisions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EditRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetType targetType;

    private EditRevision(
            TargetType targetType,
            Long targetId,
            Long memberId,
            String title,
            String content,
            String imageUrl
    ) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.memberId = memberId;
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

    public static EditRevision fromPost(Post post) {
        return new EditRevision(
                TargetType.POST,
                post.getId(),
                post.getMember().getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl()
        );
    }

    public static EditRevision fromComment(Comment comment) {
        return new EditRevision(
                TargetType.COMMENT,
                comment.getId(),
                comment.getMember().getId(),
                null,
                comment.getContent(),
                null
        );
    }
}
