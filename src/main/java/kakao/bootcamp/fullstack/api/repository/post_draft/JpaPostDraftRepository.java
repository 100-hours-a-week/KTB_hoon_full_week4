package kakao.bootcamp.fullstack.api.repository.post_draft;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post_draft.DraftStatus;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPostDraftRepository extends JpaRepository<PostDraft, Long> {

    Optional<PostDraft> findByIdAndDeletedFalseAndStatus(Long id, DraftStatus status);

    List<PostDraft> findByMember_IdAndDeletedFalseAndStatus(Long memberId, DraftStatus status);
}
