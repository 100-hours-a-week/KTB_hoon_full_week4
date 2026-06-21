package kakao.bootcamp.fullstack.api.repository.comment;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaCommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndDeletedFalse(Long id);
    List<Comment> findByPost_IdAndDeletedFalseOrderByCreatedAtAsc(Long postId);
}
