package kakao.bootcamp.fullstack.api.dto.response;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public record PostSummaryPageResDto(
        List<PostSummaryResDto> data, Long nextCursor, boolean hasNext) {

    public static PostSummaryPageResDto of(List<Post> posts, Long size) {
        boolean hasNext = posts.size() > size;
        List<Post> page = hasNext ? posts.subList(0, size.intValue()) : posts;
        List<PostSummaryResDto> summaries = page.stream().map(PostSummaryResDto::from).toList();
        Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
        return new PostSummaryPageResDto(summaries, nextCursor, hasNext);
    }
}
