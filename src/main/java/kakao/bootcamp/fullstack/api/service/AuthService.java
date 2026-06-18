package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.hasher.PasswordEncoder;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final TokenBlacklist tokenBlacklist;

    public LoginResDto login(LoginReqDto request) {
        Member member = loadMemberOrThrow(request);
        validatePasswordMatches(request.password(), member.getEncodedPassword());
        String accessToken = jwtProvider.createAccessToken(member.getId(), member.getEmail());
        return new LoginResDto(accessToken);
    }

    public void logout(String accessToken) {
        String jti = jwtProvider.getJti(accessToken);
        long expirationMillis = jwtProvider.getExpirationMillis(accessToken);
        tokenBlacklist.add(jti, expirationMillis);
    }

    private Member loadMemberOrThrow(LoginReqDto request) {
        return memberRepository.findActiveByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    private void validatePasswordMatches(String rawPassword, String encodedPassword) {
        if (!passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new UnauthorizedException(AuthErrorCode.PASSWORD_MISMATCH);
        }
    }
}
