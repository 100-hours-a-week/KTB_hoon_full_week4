package kakao.bootcamp.fullstack.api.repository.post;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post.Comment;

public interface CommentRepository {
    void save(Comment comment);
    Optional<Comment> findById(Long commentId);
    List<Comment> findByPostId(Long postId);
}
