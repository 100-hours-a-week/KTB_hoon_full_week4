package kakao.bootcamp.fullstack.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;
import kakao.bootcamp.fullstack.api.domain.search.SearchErrorCode;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final SearchRepository searchRepository;
    private final MemberRepository memberRepository;

    public PostSummaryPageResDto searchPosts(
            Long memberId,
            String keyword,
            PostCategory category,
            MeetingType meetingType,
            RecruitStatus recruitStatus,
            String sido,
            String sigungu,
            LocalDate from,
            LocalDate to,
            Long cursor,
            Long size) {
        loadMemberOrThrow(memberId);
        checkDateRange(from, to);
        PostSearchCond cond =
                new PostSearchCond(
                        requireKeyword(keyword),
                        category,
                        meetingType,
                        recruitStatus,
                        blankToNull(sido),
                        blankToNull(sigungu),
                        startOfDay(from),
                        startOfNextDay(to),
                        cursor,
                        size + 1);
        return PostSummaryPageResDto.of(searchRepository.searchPostPage(cond), size);
    }

    private String requireKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException(SearchErrorCode.SEARCH_KEYWORD_REQUIRED);
        }
        return keyword.trim();
    }

    private void checkDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException(SearchErrorCode.INVALID_DATE_RANGE);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private LocalDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private void loadMemberOrThrow(Long memberId) {
        memberRepository
                .findActiveById(memberId)
                .orElseThrow(() -> new UnauthorizedException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
