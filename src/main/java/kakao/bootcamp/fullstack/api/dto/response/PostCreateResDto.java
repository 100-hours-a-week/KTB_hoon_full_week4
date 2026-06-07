package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostCreateResDto(
        Long postId
) {
    public static PostCreateResDto from(Post post) {
        return new PostCreateResDto(post.getId());
    }
}
