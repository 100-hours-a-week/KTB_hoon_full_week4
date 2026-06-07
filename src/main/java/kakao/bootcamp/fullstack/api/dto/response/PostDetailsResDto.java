package kakao.bootcamp.fullstack.api.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostDetailsResDto(
        Long postId,
        String title,
        String content,
        long likeCount,
        long viewCount,
        Long writerId,
        String writerNickname,
        String imageUrl,
        boolean isMine,
        boolean isBlind,
        boolean isLikedByMe,
        LocalDateTime createdAt,
        List<CommentResDto> comments
) {
    public static PostDetailsResDto from(Post post, boolean isMine, boolean isLikedByMe, List<CommentResDto> comments) {
        return new PostDetailsResDto(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getLikeCount(),
                post.getViewCount(),
                post.getWriter().getId(),
                post.getWriter().getNickname(),
                post.getImageUrl(),
                isMine,
                post.isBlinded(),
                isLikedByMe,
                post.getCreatedAt(),
                comments
        );
    }
}