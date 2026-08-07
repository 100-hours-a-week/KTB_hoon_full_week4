package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;

public record PostRecruitStatusResDto(Long postId, RecruitStatus recruitStatus) {
    public static PostRecruitStatusResDto from(Post post) {
        return new PostRecruitStatusResDto(post.getId(), post.getRecruitStatus());
    }
}
