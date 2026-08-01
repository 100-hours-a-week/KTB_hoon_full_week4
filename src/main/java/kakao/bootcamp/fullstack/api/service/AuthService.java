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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        blacklistAccessToken(accessToken);
        revokeSession(refreshToken);
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            jwtProvider.validateToken(accessToken);
        } catch (UnauthorizedException e) {
            return;
        }
        tokenBlacklist.add(
                jwtProvider.getJti(accessToken), jwtProvider.getExpirationMillis(accessToken));
    }

    private void revokeSession(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository
                .findActiveByTokenHash(refreshTokenHasher.hash(rawRefreshToken))
                .ifPresent(this::revokeFamilyForLogout);
    }

    private void revokeFamilyForLogout(RefreshToken refreshToken) {
        String familyId = refreshToken.getFamilyId();
        sessionBlacklist.add(familyId, toEpochMillis(refreshToken.getExpiresAt()));
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
            // 응답 코드는 INVALID_REFRESH_TOKEN으로 통일해 추측을 막고(설계 3-3),
            // 실제 사유(이미 회전된 RT 재사용)는 서버 로그로만 남긴다.
            log.warn(
                    "RT 재사용 감지: 이미 회전된 토큰이 제출되어 family 전체를 폐기한다. familyId={}",
                    refreshToken.getFamilyId());
            revokeFamily(refreshToken.getFamilyId());
            throw new UnauthorizedException(AuthErrorCode.INVALID_REFRESH_TOKEN);
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
