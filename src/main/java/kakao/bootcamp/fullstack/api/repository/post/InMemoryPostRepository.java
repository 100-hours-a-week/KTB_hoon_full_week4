package kakao.bootcamp.fullstack.api.repository.post;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
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
        if(post.isNew()){
            Long id = idGenerator.nextId();
            post.assignId(id);
        }
        posts.put(post.getId(), post);
    }

    @Override
    public Optional<Post> findById(Long id) {
        return Optional.ofNullable(posts.get(id));
    }

    @Override
    public List<Post> findFirstPage(Long size) {
        return posts.values()
                .stream()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparingLong(Post::getId).reversed()))
                .limit(size)
                .collect(Collectors.toList());
    }

    @Override
    public List<Post> findNextPage(LocalDateTime cursorCreatedAt, Long cursorId, Long size) {
        return posts.values()
                .stream()
                .filter(post ->
                        post.getCreatedAt().isBefore(cursorCreatedAt) ||
                                (post.getCreatedAt().isEqual(cursorCreatedAt) && post.getId() < cursorId)
                )
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed()
                        .thenComparing(Comparator.comparingLong(Post::getId).reversed()))
                .limit(size)
                .collect(Collectors.toList());
    }
}
