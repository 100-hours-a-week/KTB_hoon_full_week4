package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    private Long id;
    private Long postId;
    private Long writerId;
    private Long parentCommentId;
    private String content;
    private String writerNickname;
    private boolean edited;
    private boolean deleted;

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new IllegalStateException("이미 ID가 할당된 댓글입니다.");
        }
        this.id = id;
    }
}
