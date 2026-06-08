package kakao.bootcamp.fullstack.api.domain.post_draft;

import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostDraft extends BaseEntity {

    private Long id;
    private Long writerId;
    private String title;
    private String content;
    private String imageUrl;
    private DraftStatus status;

    private PostDraft(Long writerId, String title, String content, String imageUrl) {
        this.writerId = writerId;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.status = DraftStatus.DRAFT;
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

    public boolean isWriter(Long memberId) {
        return writerId.equals(memberId);
    }

    public void update(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        updateModifiedTime();
    }

    public static PostDraft create(Long writerId, String title, String content, String imageUrl) {
        return new PostDraft(writerId, title, content, imageUrl);
    }
}
