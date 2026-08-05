package kakao.bootcamp.fullstack.post;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import kakao.bootcamp.fullstack.api.controller.PostController;
import kakao.bootcamp.fullstack.api.dto.response.PostSummaryPageResDto;
import kakao.bootcamp.fullstack.api.service.PostService;
import kakao.bootcamp.fullstack.global.config.SecurityConfig;
import kakao.bootcamp.fullstack.global.rate_limiter.RateLimiter;
import kakao.bootcamp.fullstack.global.security.filter.JwtAccessDeniedHandler;
import kakao.bootcamp.fullstack.global.security.filter.JwtAuthenticationEntryPoint;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.security.WithMockAuthMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PostController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
public class PostControllerTest {

    private static final long MAX_PAGE_SIZE = 10L;

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PostService postService;
    @MockitoBean private JwtProvider jwtProvider;
    @MockitoBean private TokenBlacklist tokenBlacklist;
    @MockitoBean private SessionBlacklist sessionBlacklist;
    @MockitoBean private RateLimiter rateLimiter;

    @Nested
    @DisplayName("GET /api/v1/posts")
    class GetPosts {

        @Test
        @WithMockAuthMember
        @DisplayName("size가 허용 범위 안이면 목록을 반환한다")
        void acceptsSizeWithinRange() throws Exception {
            // given
            given(postService.getPostSummariesList(any(), any(), any()))
                    .willReturn(new PostSummaryPageResDto(List.of(), null, false));

            // when & then
            mockMvc.perform(get("/api/v1/posts").param("size", String.valueOf(MAX_PAGE_SIZE)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockAuthMember
        @DisplayName("size가 상한을 넘으면 400 INVALID_PAGE_SIZE를 응답한다")
        void rejectsSizeAboveMax() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts").param("size", String.valueOf(MAX_PAGE_SIZE + 1)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
        }

        @Test
        @WithMockAuthMember
        @DisplayName("size가 0이면 400 INVALID_PAGE_SIZE를 응답한다")
        void rejectsZeroSize() throws Exception {
            // when & then
            mockMvc.perform(get("/api/v1/posts").param("size", "0"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
        }
    }
}
