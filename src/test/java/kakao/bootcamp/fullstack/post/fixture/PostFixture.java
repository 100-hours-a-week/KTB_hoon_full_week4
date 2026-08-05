package kakao.bootcamp.fullstack.post.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public class PostFixture {

    public static final String TITLE = "제목";
    public static final String CONTENT = "본문";
    public static final String IMAGE_URL = "https://cdn.example.com/post.png";

    public static Post post(Member writer) {
        return Post.create(writer, TITLE, CONTENT, IMAGE_URL);
    }

    public static Post post(Long id, Member writer) {
        Post post = post(writer);
        post.assignId(id);
        return post;
    }
}
