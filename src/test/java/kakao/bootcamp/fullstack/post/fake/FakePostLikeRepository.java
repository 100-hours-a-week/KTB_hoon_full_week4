package kakao.bootcamp.fullstack.post.fake;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.repository.post.PostLikeRepository;

public class FakePostLikeRepository implements PostLikeRepository {

    private final Map<Long, PostLike> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(PostLike postLike) {
        if (postLike.isNew()) {
            postLike.assignId(sequence.incrementAndGet());
        }
        store.put(postLike.getId(), postLike);
    }

    @Override
    public Optional<PostLike> findActiveByPostIdAndMemberId(Long postId, Long memberId) {
        return store.values().stream()
                .filter(postLike -> !postLike.isDeleted())
                .filter(postLike -> Objects.equals(postLike.getPost().getId(), postId))
                .filter(postLike -> Objects.equals(postLike.getMember().getId(), memberId))
                .findFirst();
    }

    @Override
    public boolean existsByPostIdAndMemberId(Long postId, Long memberId) {
        return findActiveByPostIdAndMemberId(postId, memberId).isPresent();
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
