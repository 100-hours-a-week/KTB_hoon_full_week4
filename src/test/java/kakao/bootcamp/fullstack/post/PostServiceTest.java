package kakao.bootcamp.fullstack.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import kakao.bootcamp.fullstack.api.domain.edit_revision.EditRevision;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;
import kakao.bootcamp.fullstack.api.dto.request.PostUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.PostRecruitStatusResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostUpdateResDto;
import kakao.bootcamp.fullstack.api.service.PostService;
import kakao.bootcamp.fullstack.comment.fake.FakeCommentRepository;
import kakao.bootcamp.fullstack.edit_revision.fake.FakeEditRevisionRepository;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.ForbiddenException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.post.fake.FakePostLikeRepository;
import kakao.bootcamp.fullstack.post.fake.FakePostRepository;
import kakao.bootcamp.fullstack.post.fake.FakePostViewLogRepository;
import kakao.bootcamp.fullstack.post.fixture.PostFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PostServiceTest {

    private static final Long WRITER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long MISSING_POST_ID = 999L;

    private final FakePostRepository postRepository = new FakePostRepository();
    private final FakeCommentRepository commentRepository = new FakeCommentRepository();
    private final FakeMemberRepository memberRepository = new FakeMemberRepository();
    private final FakePostLikeRepository postLikeRepository = new FakePostLikeRepository();
    private final FakeEditRevisionRepository editRevisionRepository =
            new FakeEditRevisionRepository();
    private final FakePostViewLogRepository postViewLogRepository = new FakePostViewLogRepository();

    private PostService postService;
    private Member writer;

    @BeforeEach
    void setUp() {
        postRepository.clear();
        commentRepository.clear();
        memberRepository.clear();
        postLikeRepository.clear();
        editRevisionRepository.clear();
        postViewLogRepository.clear();

        postService =
                new PostService(
                        postRepository,
                        commentRepository,
                        memberRepository,
                        postLikeRepository,
                        editRevisionRepository,
                        postViewLogRepository);

        writer = MemberFixture.activeMember(WRITER_ID);
        memberRepository.save(writer);
        memberRepository.save(
                MemberFixture.withEmailAndNickname(OTHER_MEMBER_ID, "other@example.com", "다른유저"));
    }

    private Post givenPost() {
        Post post = PostFixture.post(POST_ID, writer);
        postRepository.save(post);
        return post;
    }

    @Nested
    @DisplayName("closeRecruiting")
    class CloseRecruiting {

        @Test
        @DisplayName("모집을 마감하면 recruitStatus가 CLOSED가 된다")
        void closesRecruitingPost() {
            // given
            givenPost();

            // when
            PostRecruitStatusResDto response = postService.closeRecruiting(WRITER_ID, POST_ID);

            // then
            assertThat(response.recruitStatus()).isEqualTo(RecruitStatus.CLOSED);
            assertThat(postRepository.findActiveById(POST_ID).orElseThrow().getRecruitStatus())
                    .isEqualTo(RecruitStatus.CLOSED);
        }

        @Test
        @DisplayName("이미 CLOSED인 글을 다시 마감해도 에러 없이 멱등하게 처리된다")
        void isIdempotentWhenAlreadyClosed() {
            // given
            givenPost();
            postService.closeRecruiting(WRITER_ID, POST_ID);

            // when
            PostRecruitStatusResDto response = postService.closeRecruiting(WRITER_ID, POST_ID);

            // then
            assertThat(response.recruitStatus()).isEqualTo(RecruitStatus.CLOSED);
        }

        @Test
        @DisplayName("작성자가 아니면 NOT_POST_WRITER 예외를 던진다")
        void rejectsNonWriter() {
            // given
            givenPost();

            // when & then
            assertThatExceptionOfType(ForbiddenException.class)
                    .isThrownBy(() -> postService.closeRecruiting(OTHER_MEMBER_ID, POST_ID))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostErrorCode.NOT_POST_WRITER);
        }

        @Test
        @DisplayName("없는 게시글이면 POST_NOT_FOUND 예외를 던진다")
        void rejectsMissingPost() {
            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> postService.closeRecruiting(WRITER_ID, MISSING_POST_ID))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostErrorCode.POST_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updatePost")
    class UpdatePost {

        @Test
        @DisplayName("수정하면 이력에는 수정 전 모집 정보가, 게시글에는 수정 후 모집 정보가 남는다")
        void snapshotsRecruitmentFieldsOnUpdate() {
            // given
            givenPost();
            PostUpdateReqDto request =
                    new PostUpdateReqDto(
                            "새 제목",
                            "새 본문",
                            "https://cdn.example.com/new.png",
                            PostCategory.STUDY,
                            MeetingType.ONLINE,
                            null,
                            "새 장소",
                            10);

            // when
            PostUpdateResDto response = postService.updatePost(WRITER_ID, POST_ID, request);

            // then
            EditRevision revision = editRevisionRepository.findAll().get(0);
            assertThat(revision.getCategory()).isEqualTo(PostFixture.CATEGORY);
            assertThat(revision.getMeetingType()).isEqualTo(PostFixture.MEETING_TYPE);
            assertThat(revision.getPlaceName()).isEqualTo(PostFixture.PLACE_NAME);
            assertThat(revision.getCapacity()).isEqualTo(PostFixture.CAPACITY);
            assertThat(revision.getAddress().getSido()).isEqualTo(PostFixture.SIDO);

            Post post = postRepository.findActiveById(response.postId()).orElseThrow();
            assertThat(post.getCategory()).isEqualTo(PostCategory.STUDY);
            assertThat(post.getMeetingType()).isEqualTo(MeetingType.ONLINE);
            assertThat(post.getPlaceName()).isEqualTo("새 장소");
            assertThat(post.getCapacity()).isEqualTo(10);
            assertThat(post.getAddress()).isNull();
        }
    }
}
