package kakao.bootcamp.fullstack.api.dto.response;

import static kakao.bootcamp.fullstack.global.constants.PostConstants.BLINDED_POST;
import static kakao.bootcamp.fullstack.global.constants.PostConstants.UNKNOWN_WRITER;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostSummaryResDto(
        Long postId,
        String title,
        long likeCount,
        long commentCount,
        long viewCount,
        boolean isEdited,
        boolean isBlind,
        Long memberId,
        String writerNickname,
        LocalDateTime createdAt
) {
    public static PostSummaryResDto from(Post post) {
        return new PostSummaryResDto(
                post.getId(),
                post.isBlinded() ? BLINDED_POST : post.getTitle(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                post.isEdited(),
                post.isBlinded(),
                post.getMember().getId(),
                post.isWriterWithdrawn() ? UNKNOWN_WRITER : post.getMember().getNickname(),
                post.getCreatedAt()
        );
    }
}
