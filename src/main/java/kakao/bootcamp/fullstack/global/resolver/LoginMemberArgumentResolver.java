package kakao.bootcamp.fullstack.global.resolver;

import jakarta.servlet.http.HttpServletRequest;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.AuthMember;
import kakao.bootcamp.fullstack.global.constants.JwtConstants;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.jwt.annotation.LoginMember;
import kakao.bootcamp.fullstack.global.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.global.utils.TokenExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtProvider jwtProvider;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginMember.class)
                && parameter.getParameterType().equals(AuthMember.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
        String token = TokenExtractor.extractBearerToken(request.getHeader(JwtConstants.TOKEN_HEADER));
        if (token == null) {
            throw new UnauthorizedException(AuthErrorCode.TOKEN_EMPTY);
        }
        jwtProvider.validateToken(token);
        Long memberId = jwtProvider.getMemberId(token);
        String email = jwtProvider.getEmail(token);
        return new AuthMember(memberId, email);
    }
}
