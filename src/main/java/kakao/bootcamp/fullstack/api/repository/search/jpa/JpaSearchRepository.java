package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaSearchRepository extends JpaRepository<Post, Long> {

    @Query(
            "SELECT p FROM Post p JOIN FETCH p.member"
                    + " WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))"
                    + " OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))"
                    + " AND p.deleted = false AND p.blinded = false ORDER BY p.id DESC")
    List<Post> searchActivePostPage(@Param("keyword") String keyword, Pageable pageable);

    @Query(
            "SELECT p FROM Post p JOIN FETCH p.member"
                    + " WHERE (LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))"
                    + " OR LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')))"
                    + " AND p.id < :cursor AND p.deleted = false AND p.blinded = false ORDER BY p.id DESC")
    List<Post> searchActivePostPageBeforeCursor(
            @Param("keyword") String keyword, @Param("cursor") Long cursor, Pageable pageable);
}
