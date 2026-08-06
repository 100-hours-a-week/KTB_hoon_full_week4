package kakao.bootcamp.fullstack.post;

import kakao.bootcamp.fullstack.api.controller.PostController;
import kakao.bootcamp.fullstack.api.service.PostService;
import kakao.bootcamp.fullstack.global.config.SecurityConfig;
import kakao.bootcamp.fullstack.global.rate_limiter.RateLimiter;
import kakao.bootcamp.fullstack.global.security.filter.JwtAccessDeniedHandler;
import kakao.bootcamp.fullstack.global.security.filter.JwtAuthenticationEntryPoint;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
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
}
