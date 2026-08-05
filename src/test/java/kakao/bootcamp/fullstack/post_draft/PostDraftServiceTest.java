package kakao.bootcamp.fullstack.post_draft;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraftErrorCode;
import kakao.bootcamp.fullstack.api.dto.response.PostCreateResDto;
import kakao.bootcamp.fullstack.api.service.PostDraftService;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.ForbiddenException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.post.fake.FakePostRepository;
import kakao.bootcamp.fullstack.post.fixture.dto.PostCreateReqDtoFixture;
import kakao.bootcamp.fullstack.post_draft.fake.FakePostDraftRepository;
import kakao.bootcamp.fullstack.post_draft.fixture.PostDraftFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PostDraftServiceTest {

    private static final Long WRITER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long DRAFT_ID = 10L;
    private static final Long MISSING_DRAFT_ID = 999L;

    private final FakePostDraftRepository postDraftRepository = new FakePostDraftRepository();
    private final FakeMemberRepository memberRepository = new FakeMemberRepository();
    private final FakePostRepository postRepository = new FakePostRepository();

    private PostDraftService postDraftService;
    private Member writer;

    @BeforeEach
    void setUp() {
        postDraftRepository.clear();
        memberRepository.clear();
        postRepository.clear();

        postDraftService =
                new PostDraftService(postDraftRepository, memberRepository, postRepository);

        writer = MemberFixture.activeMember(WRITER_ID);
        memberRepository.save(writer);
        memberRepository.save(
                MemberFixture.withEmailAndNickname(OTHER_MEMBER_ID, "other@example.com", "다른유저"));
    }

    private PostDraft givenDraft() {
        PostDraft postDraft = PostDraftFixture.draft(DRAFT_ID, writer);
        postDraftRepository.save(postDraft);
        return postDraft;
    }

    private PostDraft givenPublishedDraft() {
        PostDraft postDraft = PostDraftFixture.published(DRAFT_ID, writer);
        postDraftRepository.save(postDraft);
        return postDraft;
    }

    @Nested
    @DisplayName("publishPostDraft")
    class PublishPostDraft {

        @Test
        @DisplayName("발행하면 게시글이 생성되고 임시 저장 글은 PUBLISHED가 된다")
        void publishesDraft() {
            // given
            PostDraft postDraft = givenDraft();

            // when
            PostCreateResDto response =
                    postDraftService.publishPostDraft(
                            WRITER_ID, DRAFT_ID, PostCreateReqDtoFixture.valid());

            // then
            assertThat(postDraft.isPublished()).isTrue();
            assertThat(postRepository.findActiveById(response.postId())).isPresent();
        }

        @Test
        @DisplayName("이미 발행된 임시 저장 글은 조회 대상에서 빠지므로 재발행하면 POST_DRAFT_NOT_FOUND를 던진다")
        void rejectsRepublish() {
            // given
            givenPublishedDraft();

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(
                            () ->
                                    postDraftService.publishPostDraft(
                                            WRITER_ID, DRAFT_ID, PostCreateReqDtoFixture.valid()))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostDraftErrorCode.POST_DRAFT_NOT_FOUND);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_DRAFT_WRITER 예외를 던진다")
        void rejectsNonWriter() {
            // given
            givenDraft();

            // when & then
            assertThatExceptionOfType(ForbiddenException.class)
                    .isThrownBy(
                            () ->
                                    postDraftService.publishPostDraft(
                                            OTHER_MEMBER_ID,
                                            DRAFT_ID,
                                            PostCreateReqDtoFixture.valid()))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostDraftErrorCode.NOT_POST_DRAFT_WRITER);
        }

        @Test
        @DisplayName("없는 임시 저장 글이면 POST_DRAFT_NOT_FOUND 예외를 던진다")
        void rejectsMissingDraft() {
            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(
                            () ->
                                    postDraftService.publishPostDraft(
                                            WRITER_ID,
                                            MISSING_DRAFT_ID,
                                            PostCreateReqDtoFixture.valid()))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostDraftErrorCode.POST_DRAFT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("getPostDrafts")
    class GetPostDrafts {

        @Test
        @DisplayName("발행된 임시 저장 글은 목록에서 제외된다")
        void excludesPublishedDrafts() {
            // given
            givenPublishedDraft();

            // when & then
            assertThat(postDraftService.getPostDrafts(WRITER_ID)).isEmpty();
        }

        @Test
        @DisplayName("발행하지 않은 내 임시 저장 글만 반환한다")
        void returnsOwnDrafts() {
            // given
            givenDraft();

            // when & then
            assertThat(postDraftService.getPostDrafts(WRITER_ID))
                    .extracting("draftId")
                    .containsExactly(DRAFT_ID);
            assertThat(postDraftService.getPostDrafts(OTHER_MEMBER_ID)).isEmpty();
        }
    }
}
