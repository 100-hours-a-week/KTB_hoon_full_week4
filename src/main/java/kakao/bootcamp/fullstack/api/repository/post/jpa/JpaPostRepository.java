package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN FETCH p.member WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findActiveById(@Param("id") Long id);
}
