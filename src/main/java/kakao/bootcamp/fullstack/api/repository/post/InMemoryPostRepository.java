package kakao.bootcamp.fullstack.api.repository.post;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryPostRepository implements PostRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, Post> posts = new ConcurrentHashMap<>();

    @Override
    public void save(Post post) {
        if (post.isNew()) {
            Long id = idGenerator.nextId();
            post.assignId(id);
        }
        posts.put(post.getId(), post);
    }

    @Override
    public Optional<Post> findActiveById(Long id) {
        return Optional.ofNullable(posts.get(id))
                .filter(post -> !post.isDeleted());
    }

    @Override
    public List<Post> findPage(Long cursor, Long size) {
        return posts.values().stream()
                .filter(post -> !post.isDeleted())
                .filter(post -> cursor == null || post.getId() < cursor)
                .sorted(Comparator.comparingLong(Post::getId).reversed())
                .limit(size)
                .toList();
    }
}
