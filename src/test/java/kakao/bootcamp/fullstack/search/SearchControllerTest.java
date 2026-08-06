package kakao.bootcamp.fullstack.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kakao.bootcamp.fullstack.api.controller.SearchController;
import kakao.bootcamp.fullstack.api.domain.search.SearchErrorCode;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.service.SearchService;
import kakao.bootcamp.fullstack.global.config.SecurityConfig;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.handler.GlobalExceptionHandler;
import kakao.bootcamp.fullstack.global.rate_limiter.RateLimiter;
import kakao.bootcamp.fullstack.global.security.filter.JwtAccessDeniedHandler;
import kakao.bootcamp.fullstack.global.security.filter.JwtAuthenticationEntryPoint;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.security.WithMockAuthMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@Import({
    SecurityConfig.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    GlobalExceptionHandler.class
})
public class SearchControllerTest {

    private static final String SEARCH_URL = "/api/v1/posts/search";
    private static final String KEYWORD = "농구";
    private static final long MAX_PAGE_SIZE = 10L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private SearchService searchService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private TokenBlacklist tokenBlacklist;
    @MockitoBean private SessionBlacklist sessionBlacklist;
    @MockitoBean private RateLimiter rateLimiter;

    @Test
    @WithMockAuthMember
    @DisplayName("키워드와 허용 범위의 size를 주면 검색 결과를 반환한다")
    void acceptsKeywordWithSizeWithinRange() throws Exception {
        // given
        given(searchService.searchPosts(any(), any()))
                .willReturn(new PostSummaryPageResDto(List.of(), null, false));

        // when & then
        mockMvc.perform(
                        get(SEARCH_URL)
                                .param("keyword", KEYWORD)
                                .param("size", String.valueOf(MAX_PAGE_SIZE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("size가 상한을 넘으면 400 INVALID_PAGE_SIZE를 응답한다")
    void rejectsSizeAboveMax() throws Exception {
        // when & then
        mockMvc.perform(
                        get(SEARCH_URL)
                                .param("keyword", KEYWORD)
                                .param("size", String.valueOf(MAX_PAGE_SIZE + 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("size가 0이면 400 INVALID_PAGE_SIZE를 응답한다")
    void rejectsZeroSize() throws Exception {
        // when & then
        mockMvc.perform(get(SEARCH_URL).param("keyword", KEYWORD).param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("category를 주지 않아도 검색이 동작한다")
    void acceptsMissingCategory() throws Exception {
        // given
        given(searchService.searchPosts(any(), any()))
                .willReturn(new PostSummaryPageResDto(List.of(), null, false));

        // when & then
        mockMvc.perform(get(SEARCH_URL).param("keyword", KEYWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("정의되지 않은 category를 주면 400 INVALID_ENUM_VALUE를 응답한다")
    void rejectsUnknownCategory() throws Exception {
        // when & then
        mockMvc.perform(
                        get(SEARCH_URL)
                                .param("keyword", KEYWORD)
                                .param("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ENUM_VALUE"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("여러 필터를 함께 주면 검색이 동작한다")
    void acceptsAllFilters() throws Exception {
        // given
        given(searchService.searchPosts(any(), any()))
                .willReturn(new PostSummaryPageResDto(List.of(), null, false));

        // when & then
        mockMvc.perform(
                        get(SEARCH_URL)
                                .param("keyword", KEYWORD)
                                .param("category", "EXERCISE")
                                .param("meetingType", "OFFLINE")
                                .param("recruitStatus", "RECRUITING")
                                .param("sido", "서울특별시")
                                .param("sigungu", "강남구")
                                .param("from", "2026-01-01")
                                .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("from이 to보다 뒤면 400 INVALID_DATE_RANGE를 응답한다")
    void rejectsReversedDateRange() throws Exception {
        // when & then
        mockMvc.perform(
                        get(SEARCH_URL)
                                .param("keyword", KEYWORD)
                                .param("from", "2026-12-31")
                                .param("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));
    }

    @Test
    @WithMockAuthMember
    @DisplayName("키워드가 없으면 400 SEARCH_KEYWORD_REQUIRED를 응답한다")
    void rejectsMissingKeyword() throws Exception {
        // given
        willThrow(new BadRequestException(SearchErrorCode.SEARCH_KEYWORD_REQUIRED))
                .given(searchService)
                .searchPosts(any(), any());

        // when & then
        mockMvc.perform(get(SEARCH_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SEARCH_KEYWORD_REQUIRED"));
    }
}
