package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaPostRepositoryAdapter implements PostRepository {

    private final JpaPostRepository jpaPostRepository;

    @Override
    public void save(Post post) {
        jpaPostRepository.save(post);
    }

    @Override
    public Optional<Post> findActiveById(Long id) {
        return jpaPostRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public List<Post> findPage(Long cursor, Long size) {
        Pageable pageable = PageRequest.of(0, size.intValue());
        if (cursor == null) {
            return jpaPostRepository.findByDeletedFalseOrderByIdDesc(pageable);
        }
        return jpaPostRepository.findByIdLessThanAndDeletedFalseOrderByIdDesc(cursor, pageable);
    }
}
