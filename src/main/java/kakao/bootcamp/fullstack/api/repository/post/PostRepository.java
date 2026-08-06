package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface PostRepository {
    void save(Post post);

    Optional<Post> findActiveById(Long id);
}
