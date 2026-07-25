package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostUpdateResDto(Long postId) {
    public static PostUpdateResDto from(Post post) {
        return new PostUpdateResDto(post.getId());
    }
}
