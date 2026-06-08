package kakao.bootcamp.fullstack.api.dto.response;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;

public record PostDraftsSummaryResDto(
        Long draftId,
        String title
) {
    public static PostDraftsSummaryResDto from(PostDraft postDraft) {
        return new PostDraftsSummaryResDto(
                postDraft.getId(),
                postDraft.getTitle()
        );
    }

    public static List<PostDraftsSummaryResDto> fromDomains(List<PostDraft> postDrafts) {
        return postDrafts.stream()
                .map(PostDraftsSummaryResDto::from)
                .toList();
    }
}
