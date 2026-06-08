package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;

public record PostDraftDetailsResDto(
        Long draftId,
        String title,
        String content,
        String imageUrl
) {
    public static PostDraftDetailsResDto from(PostDraft postDraft) {
        return new PostDraftDetailsResDto(
                postDraft.getId(),
                postDraft.getTitle(),
                postDraft.getContent(),
                postDraft.getImageUrl()
        );
    }
}
