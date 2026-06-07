package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
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
            throw new IllegalStateException("이미 ID가 할당된 임시글입니다.");
        }
        this.id = id;
    }

    public static PostDraft create(Long writerId, String title, String content, String imageUrl) {
        return new PostDraft(writerId, title, content, imageUrl);
    }
}