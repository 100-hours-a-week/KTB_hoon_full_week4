package kakao.bootcamp.fullstack.global.security.filter;

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
import kakao.bootcamp.fullstack.global.security.dto.AccessTokenPayload;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
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
    private final SessionBlacklist sessionBlacklist;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return Arrays.stream(PublicEndpointConstants.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> pathMatcher.match(pattern, requestUri));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token =
                TokenExtractor.extractBearerToken(request.getHeader(JwtConstants.TOKEN_HEADER));
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }
        AccessTokenPayload payload = jwtProvider.parseAccessToken(token);
        if (tokenBlacklist.exists(payload.jti())) {
            log.warn("블랙리스트에 등록된 AT 사용 시도(로그아웃된 토큰). jti={}", payload.jti());
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        }
        if (payload.fid() != null && sessionBlacklist.exists(payload.fid())) {
            log.warn("폐기된 세션의 AT 사용 시도. fid={}", payload.fid());
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        }
        AuthMember authMember = payload.toAuthMember();
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        authMember, null, List.of(new SimpleGrantedAuthority(authMember.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
