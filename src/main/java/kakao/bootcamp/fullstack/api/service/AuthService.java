package kakao.bootcamp.fullstack.api.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.dto.request.LoginReqDto;
import kakao.bootcamp.fullstack.api.dto.response.LoginResult;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.properties.JwtProperties;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;
import kakao.bootcamp.fullstack.global.security.token.RefreshTokenGenerator;
import kakao.bootcamp.fullstack.global.security.token.RefreshTokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final JwtProvider jwtProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final TokenBlacklist tokenBlacklist;
    private final SessionBlacklist sessionBlacklist;
    private final JwtProperties jwtProperties;

    @Transactional
    public LoginResult login(LoginReqDto request) {
        Member member = loadMemberOrThrow(request);
        validatePasswordMatches(request.password(), member.getEncodedPassword());
        String familyId = UUID.randomUUID().toString();
        return new LoginResult(
                issueAccessToken(member, familyId),
                issueRefreshToken(member.getId(), familyId),
                jwtProperties.refreshTokenExpireSeconds()
        );
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public LoginResult reissue(String rawRefreshToken) {
        RefreshToken refreshToken = loadActiveRefreshToken(rawRefreshToken);
        detectReuseOrThrow(refreshToken);
        validateNotExpired(refreshToken);
        Member member = loadActiveMemberOrThrow(refreshToken.getMemberId());
        return rotate(refreshToken, member);
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        tokenBlacklist.add(jwtProvider.getJti(accessToken), jwtProvider.getExpirationMillis(accessToken));
        RefreshToken storedRefreshToken = loadActiveRefreshToken(refreshToken);
        String familyId = storedRefreshToken.getFamilyId();
        sessionBlacklist.add(familyId, toEpochMillis(storedRefreshToken.getExpiresAt()));
        refreshTokenRepository.revokeAllByFamilyId(familyId);
    }

    private String issueRefreshToken(Long memberId, String familyId) {
        String rawToken = refreshTokenGenerator.generate();
        LocalDateTime expiresAt =
                LocalDateTime.now().plusSeconds(jwtProperties.refreshTokenExpireSeconds());
        refreshTokenRepository.save(
                RefreshToken.create(
                        memberId, familyId, refreshTokenHasher.hash(rawToken), expiresAt));
        return rawToken;
    }

    private String issueAccessToken(Member member, String familyId) {
        return jwtProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole(), familyId);
    }

    private LoginResult rotate(RefreshToken refreshToken, Member member) {
        refreshToken.revoke();
        String familyId = refreshToken.getFamilyId();
        String newRefreshToken = issueRefreshToken(member.getId(), familyId);
        return new LoginResult(
                issueAccessToken(member, familyId),
                newRefreshToken,
                jwtProperties.refreshTokenExpireSeconds()
        );
    }

    private RefreshToken loadActiveRefreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        return refreshTokenRepository
                .findActiveByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    private void detectReuseOrThrow(RefreshToken refreshToken) {
        if (refreshToken.isRevoked()) {
            revokeFamily(refreshToken.getFamilyId());
            throw new UnauthorizedException(AuthErrorCode.REFRESH_TOKEN_REUSE_DETECTED);
        }
    }

    private void revokeFamily(String familyId) {
        refreshTokenRepository.revokeAllByFamilyId(familyId);
        long sessionBlockExpiresAt =
                System.currentTimeMillis() + jwtProperties.accessTokenExpireSeconds() * 1000;
        sessionBlacklist.add(familyId, sessionBlockExpiresAt);
    }

    private void validateNotExpired(RefreshToken refreshToken) {
        if (refreshToken.isExpired(LocalDateTime.now())) {
            throw new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }

    private Member loadActiveMemberOrThrow(Long memberId) {
        return memberRepository
                .findActiveById(memberId)
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN));
    }

    private Member loadMemberOrThrow(LoginReqDto request) {
        return memberRepository
                .findActiveByEmail(request.email())
                .orElseThrow(() -> new UnauthorizedException(AuthErrorCode.LOGIN_FAILED));
    }

    private void validatePasswordMatches(String rawPassword, String encodedPassword) {
        if (!passwordHasher.matches(rawPassword, encodedPassword)) {
            throw new UnauthorizedException(AuthErrorCode.LOGIN_FAILED);
        }
    }

    private long toEpochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
