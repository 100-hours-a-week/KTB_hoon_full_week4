package kakao.bootcamp.fullstack.comment.fixture;

import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public class CommentFixture {

    public static final String CONTENT = "댓글 내용";

    public static Comment comment(Post post, Member writer) {
        return Comment.create(post, writer, CONTENT);
    }

    public static Comment comment(Long id, Post post, Member writer) {
        Comment comment = comment(post, writer);
        comment.assignId(id);
        return comment;
    }
}
