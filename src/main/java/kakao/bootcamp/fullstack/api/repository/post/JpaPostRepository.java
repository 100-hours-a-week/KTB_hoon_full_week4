package kakao.bootcamp.fullstack.api.repository.post;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndDeletedFalse(Long id);

    List<Post> findByDeletedFalseOrderByIdDesc(Pageable pageable);

    List<Post> findByIdLessThanAndDeletedFalseOrderByIdDesc(Long cursor, Pageable pageable);
}
