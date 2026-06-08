package kakao.bootcamp.fullstack.api.domain.edit_revision;

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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EditRevision extends BaseEntity {

    private Long id;
    private TargetType targetType;
    private Long targetId;
    private Long writerId;
    private String title;
    private String content;
    private String imageUrl;

    private EditRevision(
            TargetType targetType,
            Long targetId,
            Long writerId,
            String title,
            String content,
            String imageUrl
    ) {
        this.targetType = targetType;
        this.targetId = targetId;
        this.writerId = writerId;
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
                post.getWriter().getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl()
        );
    }

    public static EditRevision fromComment(Comment comment) {
        return new EditRevision(
                TargetType.COMMENT,
                comment.getId(),
                comment.getWriter().getId(),
                null,
                comment.getContent(),
                null
        );
    }
}
