package kakao.bootcamp.fullstack.api.repository.post_draft.inmemory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post_draft.DraftStatus;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryPostDraftRepository implements PostDraftRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, PostDraft> postDrafts = new ConcurrentHashMap<>();

    @Override
    public void save(PostDraft postDraft) {
        if(postDraft.isNew()){
            Long id = idGenerator.nextId();
            postDraft.assignId(id);
        }
        postDrafts.put(postDraft.getId(), postDraft);
    }

    @Override
    public Optional<PostDraft> findActiveById(Long id) {
        return Optional.ofNullable(postDrafts.get(id))
                .filter(postDraft -> !postDraft.isDeleted())
                .filter(postDraft -> postDraft.getStatus().equals(DraftStatus.DRAFT));
    }

    @Override
    public List<PostDraft> getPostDraftsByMemberId(Long memberId) {
        return postDrafts.values()
                .stream()
                .filter(postDraft -> postDraft.isWriter(memberId))
                .filter(postDraft -> !postDraft.isDeleted())
                .filter(postDraft -> postDraft.getStatus().equals(DraftStatus.DRAFT))
                .toList();
    }
}
