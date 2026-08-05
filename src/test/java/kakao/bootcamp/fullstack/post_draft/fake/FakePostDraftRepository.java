package kakao.bootcamp.fullstack.post_draft.fake;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.post_draft.DraftStatus;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;

public class FakePostDraftRepository implements PostDraftRepository {

    private final Map<Long, PostDraft> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(PostDraft postDraft) {
        if (postDraft.isNew()) {
            postDraft.assignId(sequence.incrementAndGet());
        }
        store.put(postDraft.getId(), postDraft);
    }

    // 프로덕션 구현(JPA·InMemory) 모두 DRAFT 상태만 반환한다. 발행된 초안은 조회되지 않는다.
    @Override
    public Optional<PostDraft> findActiveById(Long id) {
        return Optional.ofNullable(store.get(id))
                .filter(postDraft -> !postDraft.isDeleted())
                .filter(postDraft -> postDraft.getStatus() == DraftStatus.DRAFT);
    }

    @Override
    public List<PostDraft> getPostDraftsByMemberId(Long memberId) {
        return store.values().stream()
                .filter(postDraft -> postDraft.isWriter(memberId))
                .filter(postDraft -> !postDraft.isDeleted())
                .filter(postDraft -> postDraft.getStatus() == DraftStatus.DRAFT)
                .toList();
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
