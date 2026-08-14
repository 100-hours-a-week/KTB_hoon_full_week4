package kakao.bootcamp.fullstack.api.service;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;
import kakao.bootcamp.fullstack.global.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionBlacklist sessionBlacklist;
    private final PasswordHasher passwordHasher;
    private final JwtProperties jwtProperties;

    @Transactional
    public void signup(SignupReqDto request) {
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        checkEmailDuplicated(request.email());
        checkNicknameDuplicated(request.nickname());
        Member member =
                Member.create(
                        request.email(),
                        passwordHasher.hash(request.password()),
                        request.nickname(),
                        request.imageUrl());
        memberRepository.save(member);
    }

    public MemberProfileResDto getMemberProfile(Long memberId) {
        Member member = loadMemberOrThrow(memberId);
        return new MemberProfileResDto(
                member.getEmail(), member.getNickname(), member.getProfileImgUrl());
    }

    @Transactional
    public void deleteMember(Long memberId) {
        Member member = loadMemberOrThrow(memberId);
        member.delete();
        revokeAllSessions(memberId);
    }

    @Transactional
    public void updateMemberProfile(Long memberId, ProfileUpdateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        if (!member.getNickname().equals(request.nickname())) {
            checkNicknameDuplicated(request.nickname());
        }
        member.updateProfile(request.nickname(), request.imageUrl());
    }

    @Transactional
    public void updatePassword(Long memberId, PasswordUpdateReqDto request) {
        Member member = loadMemberOrThrow(memberId);
        validateCurrentPasswordMatch(request.currentPassword(), member.getEncodedPassword());
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        member.updatePassword(passwordHasher.hash(request.password()));
        revokeAllSessions(memberId);
    }

    private void revokeAllSessions(Long memberId) {
        List<String> familyIds = refreshTokenRepository.findNotRevokedFamilyIdsByMemberId(memberId);
        refreshTokenRepository.revokeAllByMemberId(memberId);
        long sessionBlockExpiresAt =
                System.currentTimeMillis() + jwtProperties.accessTokenExpireSeconds() * 1000;
        familyIds.forEach(familyId -> sessionBlacklist.add(familyId, sessionBlockExpiresAt));
    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository
                .findActiveById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateCurrentPasswordMatch(String currentPassword, String encodedPassword) {
        if (!passwordHasher.matches(currentPassword, encodedPassword)) {
            throw new BadRequestException(MemberErrorCode.CURRENT_PASSWORD_MISMATCH);
        }
    }

    private void validatePasswordConfirmMatch(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new BadRequestException(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void checkEmailDuplicated(String email) {
        if (memberRepository.existsByEmailIncludingDeleted(email)) {
            throw new BadRequestException(MemberErrorCode.EMAIL_DUPLICATED);
        }
    }

    private void checkNicknameDuplicated(String nickname) {
        if (memberRepository.existsByNicknameIncludingDeleted(nickname)) {
            throw new BadRequestException(MemberErrorCode.NICKNAME_DUPLICATED);
        }
    }
}
