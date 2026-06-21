package kakao.bootcamp.fullstack.api.repository.post_draft.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post_draft.DraftStatus;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaPostDraftRepositoryAdapter implements PostDraftRepository {

    private final JpaPostDraftRepository jpaPostDraftRepository;

    @Override
    public void save(PostDraft postDraft) {
        jpaPostDraftRepository.save(postDraft);
    }

    @Override
    public Optional<PostDraft> findActiveById(Long id) {
        return jpaPostDraftRepository.findByIdAndDeletedFalseAndStatus(id, DraftStatus.DRAFT);
    }

    @Override
    public List<PostDraft> getPostDraftsByMemberId(Long memberId) {
        return jpaPostDraftRepository.findByMember_IdAndDeletedFalseAndStatus(memberId, DraftStatus.DRAFT);
    }
}
