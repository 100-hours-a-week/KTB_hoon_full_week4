package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    private Long id;
    private Long postId;
    private Member writer;
    private Long parentCommentId;
    private String content;
    private boolean edited;
    private boolean deleted;

    private Comment(Long postId, Member writer, String content) {
        this.postId = postId;
        this.writer = writer;
        this.content = content;
    }

    public static Comment create(Long postId, Member writer, String content) {
        return new Comment(postId, writer, content);
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new IllegalStateException("이미 ID가 할당된 댓글입니다.");
        }
        this.id = id;
    }

    public void updateContent(String content) {
        this.content = content;
        if (!edited) {
            this.edited = true;
        }
    }

    public boolean isWriter(Long memberId) {
        return writer.getId().equals(memberId);
    }

    @Override
    public void delete() {
        super.delete();
        this.deleted = true;
    }
}
