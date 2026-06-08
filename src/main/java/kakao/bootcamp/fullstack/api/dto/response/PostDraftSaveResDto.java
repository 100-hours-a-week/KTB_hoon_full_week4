package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;

public record PostDraftSaveResDto(Long draftId) {
    public static PostDraftSaveResDto from(PostDraft postDraft) {
        return new PostDraftSaveResDto(postDraft.getId());
    }
}
