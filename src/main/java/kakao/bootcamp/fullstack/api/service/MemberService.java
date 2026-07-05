package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupReqDto request){
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        checkEmailDuplicated(request.email());
        checkNicknameDuplicated(request.nickname());
        Member member = Member.create(
                request.email(),
                passwordEncoder.hash(request.password()),
                request.nickname(),
                request.imageUrl()
        );
        memberRepository.save(member);
    }

    public MemberProfileResDto getMemberProfile(Long memberId){
        Member member = loadMemberOrThrow(memberId);
        return new MemberProfileResDto(
                member.getEmail(),
                member.getNickname(),
                member.getProfileImgUrl()
        );
    }

    @Transactional
    public void deleteMember(Long memberId){
        Member member = loadMemberOrThrow(memberId);
        member.delete();
    }

    @Transactional
    public void updateMemberProfile(Long memberId, ProfileUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        if (!member.getNickname().equals(request.nickname())) {
            checkNicknameDuplicated(request.nickname());
        }
        member.updateProfile(request.nickname(), request.imageUrl());
    }

    @Transactional
    public void updatePassword(Long memberId, PasswordUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        member.updatePassword(passwordEncoder.hash(request.password()));
    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findActiveById(memberId)
                .orElseThrow(() -> new NotFoundException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private void validatePasswordConfirmMatch(String password, String passwordConfirm) {
        if(!password.equals(passwordConfirm)){
            throw new BadRequestException(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void checkEmailDuplicated(String email) {
        if(memberRepository.existsByEmail(email)){
            throw new BadRequestException(MemberErrorCode.EMAIL_DUPLICATED);
        }
    }

    private void checkNicknameDuplicated(String nickname) {
        if(memberRepository.existsByNickname(nickname)){
            throw new BadRequestException(MemberErrorCode.NICKNAME_DUPLICATED);
        }
    }
}
