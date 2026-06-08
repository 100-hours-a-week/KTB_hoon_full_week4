package kakao.bootcamp.fullstack.api.repository.post;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface PostRepository {
    void save(Post post);
    Optional<Post> findActiveById(Long id);
    List<Post> findPage(Long cursor, Long size);
}
