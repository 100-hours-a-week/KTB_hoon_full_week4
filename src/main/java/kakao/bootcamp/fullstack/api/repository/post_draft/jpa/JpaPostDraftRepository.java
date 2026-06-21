package kakao.bootcamp.fullstack.api.repository.post_draft.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.post_draft.DraftStatus;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaPostDraftRepository extends JpaRepository<PostDraft, Long> {

    @Query("SELECT pd FROM PostDraft pd "
            + "WHERE pd.id = :id AND pd.deleted = false AND pd.status = :status")
    Optional<PostDraft> findActiveByIdAndStatus(@Param("id") Long id, @Param("status") DraftStatus status);

    @Query("SELECT pd FROM PostDraft pd "
            + "WHERE pd.member.id = :memberId AND pd.deleted = false AND pd.status = :status")
    List<PostDraft> findActiveByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") DraftStatus status);
}
