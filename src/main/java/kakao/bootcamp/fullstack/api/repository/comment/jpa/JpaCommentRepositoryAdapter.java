package kakao.bootcamp.fullstack.api.repository.comment.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaCommentRepositoryAdapter implements CommentRepository {

    private final JpaCommentRepository jpaCommentRepository;

    @Override
    public void save(Comment comment) {
        jpaCommentRepository.save(comment);
    }

    @Override
    public Optional<Comment> findActiveById(Long commentId) {
        return jpaCommentRepository.findByIdAndDeletedFalse(commentId);
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return jpaCommentRepository.findActiveByPostId(postId);
    }
}
