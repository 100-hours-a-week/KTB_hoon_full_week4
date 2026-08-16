package kakao.bootcamp.fullstack.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.search.SearchErrorCode;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryResDto;
import kakao.bootcamp.fullstack.api.service.SearchService;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.post.fixture.PostFixture;
import kakao.bootcamp.fullstack.search.fake.FakeSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SearchServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final LocalDateTime SAME_INSTANT =
            LocalDateTime.of(2026, 8, 9, 12, 20, 12, 5829);

    private FakeSearchRepository searchRepository;
    private FakeMemberRepository memberRepository;
    private SearchService searchService;
    private Member writer;

    @BeforeEach
    void setUp() {
        searchRepository = new FakeSearchRepository();
        memberRepository = new FakeMemberRepository();
        searchService = new SearchService(searchRepository, memberRepository);
        writer = MemberFixture.activeMember(MEMBER_ID);
        memberRepository.save(writer);
    }

    @Test
    @DisplayName("작성일 내림차순으로 정렬하고, 작성일이 같으면 id 내림차순으로 정렬한다")
    void sortsByCreatedAtThenIdDescending() {
        // given
        savePost(1L, SAME_INSTANT);
        savePost(2L, SAME_INSTANT);
        savePost(3L, SAME_INSTANT.minusDays(1));

        // when
        PostSummaryPageResDto response = search(null, 10L);

        // then
        assertThat(response.data())
                .extracting(PostSummaryResDto::postId)
                .containsExactly(2L, 1L, 3L);
    }

    @Test
    @DisplayName("다음 페이지가 없으면 hasNext는 false이고 nextCursor는 null이다")
    void returnsNullCursorWhenNoNextPage() {
        // given
        savePost(1L, SAME_INSTANT);

        // when
        PostSummaryPageResDto response = search(null, 10L);

        // then
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @Test
    @DisplayName("size보다 결과가 많으면 size개만 반환하고 nextCursor를 발급한다")
    void returnsCursorWhenNextPageExists() {
        // given
        savePost(1L, SAME_INSTANT.minusDays(2));
        savePost(2L, SAME_INSTANT.minusDays(1));
        savePost(3L, SAME_INSTANT);

        // when
        PostSummaryPageResDto response = search(null, 2L);

        // then
        assertThat(response.data()).extracting(PostSummaryResDto::postId).containsExactly(3L, 2L);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
    }

    @Test
    @DisplayName("작성일이 모두 같아도 커서로 페이지를 넘기면 누락도 중복도 없다")
    void paginatesWithoutLossOrDuplicationWhenCreatedAtIsIdentical() {
        // given
        savePost(1L, SAME_INSTANT);
        savePost(2L, SAME_INSTANT);
        savePost(3L, SAME_INSTANT);
        savePost(4L, SAME_INSTANT);
        savePost(5L, SAME_INSTANT);

        // when
        List<Long> collected = collectAllPages(2L);

        // then
        assertThat(collected).containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Test
    @DisplayName("작성일이 섞여 있어도 커서로 페이지를 넘기면 누락도 중복도 없다")
    void paginatesWithoutLossOrDuplicationWhenCreatedAtIsMixed() {
        // given
        savePost(1L, SAME_INSTANT.minusDays(3));
        savePost(2L, SAME_INSTANT.minusDays(3));
        savePost(3L, SAME_INSTANT.minusDays(1));
        savePost(4L, SAME_INSTANT);
        savePost(5L, SAME_INSTANT);

        // when
        List<Long> collected = collectAllPages(2L);

        // then
        assertThat(collected).containsExactly(5L, 4L, 3L, 2L, 1L);
    }

    @Test
    @DisplayName("서버가 발급하지 않은 커서를 주면 INVALID_CURSOR를 던진다")
    void rejectsMalformedCursor() {
        // when & then
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(() -> search("not-a-cursor", 10L))
                .extracting(BusinessException::getCode)
                .isEqualTo(SearchErrorCode.INVALID_CURSOR);
    }

    @Test
    @DisplayName("from이 to보다 뒤면 INVALID_DATE_RANGE를 던진다")
    void rejectsReversedDateRange() {
        // when & then
        assertThatExceptionOfType(BadRequestException.class)
                .isThrownBy(
                        () ->
                                searchService.searchPosts(
                                        MEMBER_ID,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        LocalDate.of(2026, 12, 31),
                                        LocalDate.of(2026, 1, 1),
                                        null,
                                        10L))
                .extracting(BusinessException::getCode)
                .isEqualTo(SearchErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    @DisplayName("to로 지정한 날짜에 작성된 글도 결과에 포함한다")
    void includesPostsWrittenOnToDate() {
        // given
        LocalDate day = LocalDate.of(2026, 8, 9);
        savePost(1L, day.atTime(23, 59, 59));

        // when
        PostSummaryPageResDto response =
                searchService.searchPosts(
                        MEMBER_ID, null, null, null, null, null, null, day, day, null, 10L);

        // then
        assertThat(response.data()).extracting(PostSummaryResDto::postId).containsExactly(1L);
    }

    @Test
    @DisplayName("존재하지 않는 회원이 요청하면 MEMBER_NOT_FOUND를 던진다")
    void rejectsUnknownMember() {
        // when & then
        assertThatExceptionOfType(UnauthorizedException.class)
                .isThrownBy(
                        () ->
                                searchService.searchPosts(
                                        999L, null, null, null, null, null, null, null, null, null,
                                        10L))
                .extracting(BusinessException::getCode)
                .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
    }

    private void savePost(Long id, LocalDateTime createdAt) {
        searchRepository.save(PostFixture.post(id, writer, createdAt));
    }

    private PostSummaryPageResDto search(String cursor, Long size) {
        return searchService.searchPosts(
                MEMBER_ID, null, null, null, null, null, null, null, null, cursor, size);
    }

    private List<Long> collectAllPages(Long size) {
        List<Long> collected = new ArrayList<>();
        String cursor = null;
        do {
            PostSummaryPageResDto page = search(cursor, size);
            page.data().forEach(post -> collected.add(post.postId()));
            cursor = page.nextCursor();
        } while (cursor != null);
        return collected;
    }
}
