package kakao.bootcamp.fullstack.comment.fake;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;

public class FakeCommentRepository implements CommentRepository {

    private final Map<Long, Comment> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Comment comment) {
        if (comment.isNew()) {
            comment.assignId(sequence.incrementAndGet());
        }
        store.put(comment.getId(), comment);
    }

    @Override
    public Optional<Comment> findActiveById(Long commentId) {
        return Optional.ofNullable(store.get(commentId)).filter(comment -> !comment.isDeleted());
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return store.values().stream()
                .filter(comment -> !comment.isDeleted())
                .filter(comment -> Objects.equals(comment.getPost().getId(), postId))
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .toList();
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
