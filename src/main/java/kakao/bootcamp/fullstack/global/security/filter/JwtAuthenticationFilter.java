package kakao.bootcamp.fullstack.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.global.constants.JwtConstants;
import kakao.bootcamp.fullstack.global.constants.PublicEndpointConstants;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.global.utils.TokenExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return Arrays.stream(PublicEndpointConstants.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

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
                throw new UnauthorizedException(AuthErrorCode.TOKEN_BLACKLISTED);
            }
            Long memberId = jwtProvider.getMemberId(token);
            String email = jwtProvider.getEmail(token);
            String role = jwtProvider.getRole(token).name();
            AuthMember authMember = new AuthMember(memberId, email, role);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    authMember,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + authMember.role()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        }catch (UnauthorizedException e) {
            throw e;
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        }
    }
}
