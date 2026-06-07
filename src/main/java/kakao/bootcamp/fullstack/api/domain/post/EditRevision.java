package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EditRevision extends BaseEntity {

    private Long id;
    private RevisionTargetType targetType;
    private Long targetId;
    private Long writerId;
    private String title;
    private String content;
    private String imageUrl;

    private EditRevision(
            RevisionTargetType targetType,
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
            throw new IllegalStateException("이미 ID가 할당된 수정 이력입니다.");
        }
        this.id = id;
    }

    public static EditRevision fromPost(Post post) {
        return new EditRevision(
                RevisionTargetType.POST,
                post.getId(),
                post.getWriterId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl()
        );
    }

    public static EditRevision fromComment(Comment comment) {
        return new EditRevision(
                RevisionTargetType.COMMENT,
                comment.getId(),
                comment.getWriterId(),
                null,
                comment.getContent(),
                null
        );
    }
}
