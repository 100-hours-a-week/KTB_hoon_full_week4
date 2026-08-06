package kakao.bootcamp.fullstack.api.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.search.SearchErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PostSearchReqDto;
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

    public PostSummaryPageResDto searchPosts(Long memberId, PostSearchReqDto request) {
        loadMemberOrThrow(memberId);
        List<Post> posts = searchRepository.searchPostPage(toCond(request));
        return PostSummaryPageResDto.of(posts, request.size());
    }

    private PostSearchCond toCond(PostSearchReqDto request) {
        return new PostSearchCond(
                requireKeyword(request.keyword()),
                request.category(),
                request.meetingType(),
                request.recruitStatus(),
                blankToNull(request.sido()),
                blankToNull(request.sigungu()),
                startOfDay(request.from()),
                startOfNextDay(request.to()),
                request.cursor(),
                request.size() + 1);
    }

    private String requireKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException(SearchErrorCode.SEARCH_KEYWORD_REQUIRED);
        }
        return keyword.trim();
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
