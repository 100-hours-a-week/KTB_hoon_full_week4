package kakao.bootcamp.fullstack.api.repository.post.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.id = :id AND p.deleted = false")
    Optional<Post> findActiveById(@Param("id") Long id);

    @Query("SELECT p FROM Post p WHERE p.deleted = false ORDER BY p.id DESC")
    List<Post> findActivePage(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.id < :cursor AND p.deleted = false ORDER BY p.id DESC")
    List<Post> findActivePageBeforeCursor(@Param("cursor") Long cursor, Pageable pageable);
}
