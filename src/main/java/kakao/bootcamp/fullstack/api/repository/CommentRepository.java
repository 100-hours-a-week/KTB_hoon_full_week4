package kakao.bootcamp.fullstack.api.repository;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Comment;

public interface CommentRepository {
    void save(Comment comment);
    List<Comment> findByPostId(Long postId);

}
