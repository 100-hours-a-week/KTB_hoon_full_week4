package kakao.bootcamp.fullstack.api.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface PostRepository {
    void save(Post post);
    Optional<Post> findById(Long id);
    List<Post> findFirstPage(Long size);
    List<Post> findNextPage(LocalDateTime cursorCreatedAt, Long cursorId, Long size);
}
