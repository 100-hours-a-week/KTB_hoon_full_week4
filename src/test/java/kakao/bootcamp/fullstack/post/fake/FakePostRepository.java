package kakao.bootcamp.fullstack.post.fake;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;

public class FakePostRepository implements PostRepository {

    private final Map<Long, Post> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Post post) {
        if (post.isNew()) {
            post.assignId(sequence.incrementAndGet());
        }
        store.put(post.getId(), post);
    }

    @Override
    public Optional<Post> findActiveById(Long id) {
        return Optional.ofNullable(store.get(id)).filter(post -> !post.isDeleted());
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
