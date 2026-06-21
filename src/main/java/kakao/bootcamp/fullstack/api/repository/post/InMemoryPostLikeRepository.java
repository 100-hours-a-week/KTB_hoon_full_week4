package kakao.bootcamp.fullstack.api.repository.post;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
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
    public void delete(Long postId, Long memberId) {
        likes.values().removeIf(like ->
                like.getPost().getId().equals(postId) && like.getMember().getId().equals(memberId)
        );
    }

    @Override
    public boolean existsByPostIdAndMemberId(Long postId, Long memberId) {
        return likes.values()
                .stream()
                .anyMatch(like ->
                        like.getPost().getId().equals(postId) && like.getMember().getId().equals(memberId)
                );
    }
}
