package kakao.bootcamp.fullstack.api.repository.comment.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
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
    public Optional<Comment> findActiveById(Long commentId) {
        return Optional.ofNullable(comments.get(commentId))
                .filter(comment -> !comment.isDeleted());
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return comments.values()
                .stream()
                .filter(comment -> !comment.isDeleted())
                .filter(comment -> Objects.equals(comment.getPost().getId(), postId))
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .toList();
    }
}
