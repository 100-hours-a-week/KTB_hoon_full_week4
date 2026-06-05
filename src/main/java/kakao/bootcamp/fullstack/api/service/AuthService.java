package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResDto;
import kakao.bootcamp.fullstack.api.repository.MemberRepository;
import kakao.bootcamp.fullstack.global.config.PasswordHasher;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.jwt.provider.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;
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
        return memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    private void validatePasswordMatches(String rawPassword, String encodedPassword) {
        if (!passwordHasher.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(AuthErrorCode.PASSWORD_MISMATCH);
        }
    }
}
