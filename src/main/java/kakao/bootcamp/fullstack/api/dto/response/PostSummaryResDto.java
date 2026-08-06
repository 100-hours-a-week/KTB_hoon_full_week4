package kakao.bootcamp.fullstack.api.dto.response;

import static kakao.bootcamp.fullstack.global.constants.PostConstants.BLINDED_POST;
import static kakao.bootcamp.fullstack.global.constants.PostConstants.UNKNOWN_WRITER;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;

public record PostSummaryResDto(
        Long postId,
        String title,
        PostCategory category,
        MeetingType meetingType,
        AddressResDto address,
        String placeName,
        RecruitStatus recruitStatus,
        Integer capacity,
        long likeCount,
        long commentCount,
        long viewCount,
        boolean isEdited,
        boolean isBlind,
        Long memberId,
        String writerNickname,
        LocalDateTime createdAt) {
    public static PostSummaryResDto from(Post post) {
        return new PostSummaryResDto(
                post.getId(),
                post.isBlinded() ? BLINDED_POST : post.getTitle(),
                post.getCategory(),
                post.getMeetingType(),
                AddressResDto.from(post.getAddress()),
                post.getPlaceName(),
                post.getRecruitStatus(),
                post.getCapacity(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                post.isEdited(),
                post.isBlinded(),
                post.getMember().getId(),
                post.isWriterWithdrawn() ? UNKNOWN_WRITER : post.getMember().getNickname(),
                post.getCreatedAt());
    }
}
