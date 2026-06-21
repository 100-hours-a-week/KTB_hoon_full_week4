package kakao.bootcamp.fullstack.api.repository.post.inmemory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.repository.post.PostLikeRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryPostLikeRepository implements PostLikeRepository {

    private final Map<Long, PostLike> likes = new ConcurrentHashMap<>();
    private final IdGenerator idGenerator = new AtomicLongIdGenerator();

    @Override
    public void save(PostLike postLike) {
        if (postLike.isNew()) {
            Long id = idGenerator.nextId();
            postLike.assignId(id);
        }
        likes.put(postLike.getId(), postLike);
    }

    @Override
    public Optional<PostLike> findActiveByPostIdAndMemberId(Long postId, Long memberId) {
        return likes.values()
                .stream()
                .filter(like -> !like.isDeleted()
                        && like.getPost().getId().equals(postId)
                        && like.getMember().getId().equals(memberId))
                .findFirst();
    }

    @Override
    public boolean existsByPostIdAndMemberId(Long postId, Long memberId) {
        return likes.values()
                .stream()
                .anyMatch(like -> !like.isDeleted()
                        && like.getPost().getId().equals(postId)
                        && like.getMember().getId().equals(memberId));
    }
}
