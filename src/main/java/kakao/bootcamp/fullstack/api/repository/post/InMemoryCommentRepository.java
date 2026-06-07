package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.Comment;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCommentRepository implements CommentRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, Comment> comments = new ConcurrentHashMap<>();

    @Override
    public void save(Comment comment) {
        if (comment.isNew()) {
            Long id = idGenerator.nextId();
            comment.assignId(id);
        }
        comments.put(comment.getId(), comment);
    }

    @Override
    public Optional<Comment> findById(Long commentId) {
        return Optional.ofNullable(comments.get(commentId));
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return comments.values()
                .stream()
                .filter(comment -> Objects.equals(comment.getPostId(), postId))
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .toList();
    }
}
