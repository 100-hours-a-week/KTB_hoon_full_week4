package kakao.bootcamp.fullstack.api.service;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.search.SearchErrorCode;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
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
            Long memberId, String keyword, PostCategory category, Long cursor, Long size) {
        loadMemberOrThrow(memberId);
        List<Post> posts =
                searchRepository.searchPostPage(
                        requireKeyword(keyword), category, cursor, size + 1);
        return PostSummaryPageResDto.of(posts, size);
    }

    private String requireKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new BadRequestException(SearchErrorCode.SEARCH_KEYWORD_REQUIRED);
        }
        return keyword.trim();
    }

    private void loadMemberOrThrow(Long memberId) {
        memberRepository
                .findActiveById(memberId)
                .orElseThrow(() -> new UnauthorizedException(MemberErrorCode.MEMBER_NOT_FOUND));
    }
}
