package kakao.bootcamp.fullstack.api.repository.post_draft;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;

public interface PostDraftRepository {
    void save(PostDraft postDraft);
    Optional<PostDraft> findActiveById(Long id);
    List<PostDraft> getPostDraftsByMemberId(Long memberId);
}
