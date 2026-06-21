package kakao.bootcamp.fullstack.api.repository.comment.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaCommentRepository extends JpaRepository<Comment, Long> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.id = :id AND c.deleted = false")
    Optional<Comment> findByIdAndDeletedFalse(@Param("id") Long id);

    @Query("SELECT c FROM Comment c JOIN FETCH c.member WHERE c.post.id = :postId AND c.deleted = false ORDER BY c.createdAt ASC")
    List<Comment> findActiveByPostId(@Param("postId") Long postId);
}
