package kakao.bootcamp.fullstack.api.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.Comment;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCommentRepository implements CommentRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, List<Comment>> comments = new ConcurrentHashMap<>();

    @Override
    public void save(Comment comment) {
        if(comment.isNew()){
            Long id = idGenerator.nextId();
            comment.assignId(id);
        }
        comments.computeIfAbsent(comment.getPostId(), k -> new ArrayList<>()).add(comment);
    }

    @Override
    public List<Comment> findByPostId(Long postId) {
        return comments.get(postId);
    }
}
