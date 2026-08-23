package kakao.bootcamp.fullstack.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.comment.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.report.ReportErrorCode;
import kakao.bootcamp.fullstack.api.domain.report.ReportReason;
import kakao.bootcamp.fullstack.api.dto.request.PostReportReqDto;
import kakao.bootcamp.fullstack.api.service.ReportService;
import kakao.bootcamp.fullstack.api.service.report.CommentReportHandler;
import kakao.bootcamp.fullstack.api.service.report.PostReportHandler;
import kakao.bootcamp.fullstack.comment.fake.FakeCommentRepository;
import kakao.bootcamp.fullstack.comment.fixture.CommentFixture;
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.ConflictException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.post.fake.FakePostRepository;
import kakao.bootcamp.fullstack.post.fixture.PostFixture;
import kakao.bootcamp.fullstack.report.fake.FakeReportRepository;
import kakao.bootcamp.fullstack.search.fake.FakePostSearchIndex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class ReportServiceTest {

    private static final Long WRITER_ID = 1L;
    private static final Long REPORTER_ID = 2L;
    private static final Long POST_ID = 10L;
    private static final Long COMMENT_ID = 20L;
    private static final Long MISSING_TARGET_ID = 999L;

    private final FakeReportRepository reportRepository = new FakeReportRepository();
    private final FakeMemberRepository memberRepository = new FakeMemberRepository();
    private final FakePostRepository postRepository = new FakePostRepository();
    private final FakeCommentRepository commentRepository = new FakeCommentRepository();
    private final FakePostSearchIndex postSearchIndex = new FakePostSearchIndex();

    private ReportService reportService;
    private Member writer;
    private Member reporter;

    @BeforeEach
    void setUp() {
        reportRepository.clear();
        memberRepository.clear();
        postRepository.clear();
        commentRepository.clear();

        reportService =
                new ReportService(
                        reportRepository,
                        memberRepository,
                        List.of(
                                new PostReportHandler(postRepository, postSearchIndex),
                                new CommentReportHandler(commentRepository)));

        writer = MemberFixture.activeMember(WRITER_ID);
        reporter = MemberFixture.withEmailAndNickname(REPORTER_ID, "reporter@example.com", "리포터");
        memberRepository.save(writer);
        memberRepository.save(reporter);
    }

    private static PostReportReqDto reportPost(Long targetId) {
        return new PostReportReqDto(targetId, TargetType.POST, ReportReason.SPAM);
    }

    private static PostReportReqDto reportComment(Long targetId) {
        return new PostReportReqDto(targetId, TargetType.COMMENT, ReportReason.ABUSE);
    }

    private Post givenPostBy(Member member) {
        Post post = PostFixture.post(POST_ID, member);
        postRepository.save(post);
        return post;
    }

    private Comment givenCommentBy(Member member) {
        Comment comment = CommentFixture.comment(COMMENT_ID, givenPostBy(member), member);
        commentRepository.save(comment);
        return comment;
    }

    @Nested
    @DisplayName("report")
    class Report {

        @Test
        @DisplayName("남의 게시글을 신고하면 신고가 저장되고 대상의 신고 수가 늘어난다")
        void reportsOthersPost() {
            // given
            Post post = givenPostBy(writer);

            // when
            reportService.report(REPORTER_ID, reportPost(POST_ID));

            // then
            assertThat(post.getReportCount()).isEqualTo(1L);
            assertThat(
                            reportRepository.existsByTargetAndMember(
                                    POST_ID, TargetType.POST, REPORTER_ID))
                    .isTrue();
        }

        @Test
        @DisplayName("자기가 쓴 게시글을 신고하면 SELF_REPORT_NOT_ALLOWED 예외를 던진다")
        void rejectsSelfReportOnPost() {
            // given
            Post post = givenPostBy(writer);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> reportService.report(WRITER_ID, reportPost(POST_ID)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
            assertThat(post.getReportCount()).isZero();
        }

        @Test
        @DisplayName("자기가 쓴 댓글을 신고하면 SELF_REPORT_NOT_ALLOWED 예외를 던진다")
        void rejectsSelfReportOnComment() {
            // given
            Comment comment = givenCommentBy(writer);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> reportService.report(WRITER_ID, reportComment(COMMENT_ID)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(ReportErrorCode.SELF_REPORT_NOT_ALLOWED);
            assertThat(comment.getReportCount()).isZero();
        }

        @Test
        @DisplayName("자기 신고 검사보다 대상 존재 확인이 먼저라, 없는 게시글이면 POST_NOT_FOUND를 던진다")
        void checksTargetExistenceBeforeSelfReport() {
            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(
                            () -> reportService.report(WRITER_ID, reportPost(MISSING_TARGET_ID)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(PostErrorCode.POST_NOT_FOUND);
        }

        @Test
        @DisplayName("없는 댓글을 신고하면 COMMENT_NOT_FOUND 예외를 던진다")
        void rejectsMissingComment() {
            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(
                            () ->
                                    reportService.report(
                                            REPORTER_ID, reportComment(MISSING_TARGET_ID)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(CommentErrorCode.COMMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("같은 대상을 다시 신고하면 ALREADY_REPORTED 예외를 던지고 신고 수는 그대로다")
        void rejectsDuplicateReport() {
            // given
            Post post = givenPostBy(writer);
            reportService.report(REPORTER_ID, reportPost(POST_ID));

            // when & then
            assertThatExceptionOfType(ConflictException.class)
                    .isThrownBy(() -> reportService.report(REPORTER_ID, reportPost(POST_ID)))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(ReportErrorCode.ALREADY_REPORTED);
            assertThat(post.getReportCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("서로 다른 회원의 신고가 임계치에 도달하면 게시글이 블라인드된다")
        void blindsPostOnThreshold() {
            // given
            Post post = givenPostBy(writer);
            long threshold = PostConstants.BLIND_THRESHOLD;

            // when
            for (long i = 0; i < threshold; i++) {
                Long reporterId = REPORTER_ID + i;
                memberRepository.save(
                        MemberFixture.withEmailAndNickname(
                                reporterId, "reporter" + i + "@example.com", "리포터" + i));
                reportService.report(reporterId, reportPost(POST_ID));
            }

            // then
            assertThat(post.getReportCount()).isEqualTo(threshold);
            assertThat(post.isBlinded()).isTrue();
        }

        @Test
        @DisplayName("블라인드되는 순간에만 검색 색인에 다시 반영한다")
        void reindexesOnceWhenBlinded() {
            // given
            givenPostBy(writer);
            long threshold = PostConstants.BLIND_THRESHOLD;

            // when
            for (long i = 0; i < threshold; i++) {
                Long reporterId = REPORTER_ID + i;
                memberRepository.save(
                        MemberFixture.withEmailAndNickname(
                                reporterId, "reporter" + i + "@example.com", "리포터" + i));
                reportService.report(reporterId, reportPost(POST_ID));
            }

            // then
            assertThat(postSearchIndex.indexedPosts())
                    .singleElement()
                    .satisfies(indexed -> assertThat(indexed.isBlinded()).isTrue());
        }
    }
}
