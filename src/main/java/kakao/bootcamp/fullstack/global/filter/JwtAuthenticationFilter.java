package kakao.bootcamp.fullstack.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import kakao.bootcamp.fullstack.global.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import static kakao.bootcamp.fullstack.global.constants.JwtConstants.*;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = authorizationHeader.substring(BEARER_PREFIX.length());

        if (!jwtProvider.validateToken(accessToken)) {
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN);
            return;
        }
        String jti = jwtProvider.getJti(accessToken);
        if (tokenBlacklist.contains(jti)) {
            writeErrorResponse(response, AuthErrorCode.TOKEN_BLACKLISTED);
            return;
        }
        Long memberId = jwtProvider.getMemberId(accessToken);
        request.setAttribute(LOGIN_MEMBER_ID_ATTRIBUTE, memberId);
        request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, accessToken);
        filterChain.doFilter(request, response);
    }

    // 공통 응답까지 맞추려면 Filter에서 JSON을 직접 작성해야 함
    private void writeErrorResponse(HttpServletResponse response, BaseCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String body = """
            {
              "message": "%s",
              "code": "%s",
              "data": null
            }
            """.formatted(errorCode.getMessage(), errorCode.getCode());

        response.getWriter().write(body);
    }
}
