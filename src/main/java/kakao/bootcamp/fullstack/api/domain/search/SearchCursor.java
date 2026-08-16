package kakao.bootcamp.fullstack.api.domain.search;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;

public record SearchCursor(LocalDateTime createdAt, Long id) {

    private static final String DELIMITER = "_";

    public static SearchCursor from(Post post) {
        return new SearchCursor(post.getCreatedAt(), post.getId());
    }

    public static SearchCursor decode(String encoded) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
            int boundary = raw.lastIndexOf(DELIMITER);
            return new SearchCursor(
                    LocalDateTime.parse(raw.substring(0, boundary)),
                    Long.parseLong(raw.substring(boundary + 1)));
        } catch (RuntimeException e) {
            throw new BadRequestException(SearchErrorCode.INVALID_CURSOR);
        }
    }

    public String encode() {
        String raw = createdAt + DELIMITER + id;
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
