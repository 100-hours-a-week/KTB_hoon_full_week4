package kakao.bootcamp.fullstack.search.fake;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;

public class FakeSearchRepository implements SearchRepository {

    private final Map<Long, Post> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public void save(Post post) {
        if (post.isNew()) {
            post.assignId(sequence.incrementAndGet());
        }
        store.put(post.getId(), post);
    }

    @Override
    public List<Post> searchPostPage(
            String keyword, PostCategory category, Long cursor, Long size) {
        String lowered = keyword.toLowerCase();
        return store.values().stream()
                .filter(post -> !post.isDeleted())
                .filter(post -> !post.isBlinded())
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> cursor == null || post.getId() < cursor)
                .filter(
                        post ->
                                post.getTitle().toLowerCase().contains(lowered)
                                        || post.getContent().toLowerCase().contains(lowered))
                .sorted(Comparator.comparingLong(Post::getId).reversed())
                .limit(size)
                .toList();
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
