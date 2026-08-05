package kakao.bootcamp.fullstack.post_draft.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;

public class PostDraftFixture {

    public static final String TITLE = "초안 제목";
    public static final String CONTENT = "초안 본문";
    public static final String IMAGE_URL = "https://cdn.example.com/draft.png";

    public static PostDraft draft(Member writer) {
        return PostDraft.create(writer, TITLE, CONTENT, IMAGE_URL);
    }

    public static PostDraft draft(Long id, Member writer) {
        PostDraft postDraft = draft(writer);
        postDraft.assignId(id);
        return postDraft;
    }

    public static PostDraft published(Long id, Member writer) {
        PostDraft postDraft = draft(id, writer);
        postDraft.publish();
        return postDraft;
    }
}
