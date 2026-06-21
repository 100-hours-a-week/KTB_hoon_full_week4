package kakao.bootcamp.fullstack.api.dto.response;

import static kakao.bootcamp.fullstack.global.constants.PostConstants.BLINDED_POST;
import static kakao.bootcamp.fullstack.global.constants.PostConstants.UNKNOWN_WRITER;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostDetailsResDto(
        Long postId,
        String title,
        String content,
        long likeCount,
        long viewCount,
        Long memberId,
        String writerNickname,
        String imageUrl,
        boolean isMine,
        boolean isBlind,
        boolean isLikedByMe,
        LocalDateTime createdAt,
        List<CommentResDto> comments
) {
    public static PostDetailsResDto from(Post post, boolean isMine, boolean isLikedByMe, List<CommentResDto> comments) {
        boolean blinded = post.isBlinded();
        return new PostDetailsResDto(
                post.getId(),
                blinded ? BLINDED_POST : post.getTitle(),
                blinded ? BLINDED_POST : post.getContent(),
                post.getLikeCount(),
                post.getViewCount(),
                post.getMember().getId(),
                post.isWriterWithdrawn() ? UNKNOWN_WRITER : post.getMember().getNickname(),
                blinded ? null : post.getImageUrl(),
                isMine,
                blinded,
                isLikedByMe,
                post.getCreatedAt(),
                comments
        );
    }
}
