package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface PostRepository {
    void save(Post post);

    Optional<Post> findActiveById(Long id);

    // 소프트삭제된 글까지 포함해 조회한다. 색인 동기화 폴러가 삭제 여부를 판정할 때 쓴다.
    Optional<Post> findById(Long id);
}
