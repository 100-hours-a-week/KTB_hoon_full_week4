package kakao.bootcamp.fullstack.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.constants.JwtConstants;
import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import kakao.bootcamp.fullstack.global.utils.TokenExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {


    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = TokenExtractor.extractBearerToken(request.getHeader(JwtConstants.TOKEN_HEADER));
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            jwtProvider.validateToken(token);
            if (tokenBlacklist.exists(jwtProvider.getJti(token))) {
                writeErrorResponse(response, AuthErrorCode.TOKEN_BLACKLISTED);
                return;
            }
            Long memberId = jwtProvider.getMemberId(token);
            String email = jwtProvider.getEmail(token);
            Role role = jwtProvider.getRole(token);
            AuthMember authMember = new AuthMember(memberId, email, role);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    authMember,
                    null,
                    Collections.emptyList()
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            writeErrorResponse(response, AuthErrorCode.INVALID_TOKEN);
        }
    }

    private void writeErrorResponse(HttpServletResponse response, BaseCode code) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.error(code)
        ));
    }
}
