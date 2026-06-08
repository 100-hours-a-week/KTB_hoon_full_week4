package kakao.bootcamp.fullstack.api.repository.comment;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;

public interface CommentRepository {
    void save(Comment comment);
    Optional<Comment> findActiveById(Long commentId);
    List<Comment> findByPostId(Long postId);
}
