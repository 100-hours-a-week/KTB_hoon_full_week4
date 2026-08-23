package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaPostRepositoryAdapter implements PostRepository {

    private final JpaPostRepository jpaPostRepository;

    @Override
    public void save(Post post) {
        jpaPostRepository.save(post);
    }

    @Override
    public Optional<Post> findActiveById(Long id) {
        return jpaPostRepository.findActiveById(id);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return jpaPostRepository.findById(id);
    }
}
