package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.global.hasher.PasswordEncoder;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordHasher;

    public void signup(SignupReqDto request){
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        checkEmailDuplicated(request.email());
        checkNicknameDuplicated(request.nickname());
        Member member = Member.create(
                request.email(),
                passwordHasher.hash(request.password()),
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

    public void deleteMember(Long memberId){
        Member member = loadMemberOrThrow(memberId);
        member.delete();
    }

    public void updateMemberProfile(Long memberId, ProfileUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        if (!member.getNickname().equals(request.nickname())) {
            checkNicknameDuplicated(request.nickname());
        }
        member.updateProfile(request.nickname(), request.imageUrl());
        memberRepository.save(member);
    }

    public void updatePassword(Long memberId, PasswordUpdateReqDto request){
        Member member = loadMemberOrThrow(memberId);
        validatePasswordConfirmMatch(request.password(), request.passwordConfirm());
        member.updatePassword(passwordHasher.hash(request.password()));
        memberRepository.save(member);
    }

    private Member loadMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(AuthErrorCode.MEMBER_NOT_FOUND));
    }

    private void validatePasswordConfirmMatch(String password, String passwordConfirm) {
        if(!password.equals(passwordConfirm)){
            throw new BusinessException(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }
    }

    private void checkEmailDuplicated(String email) {
        if(memberRepository.existsByEmail(email)){
            throw new BusinessException(MemberErrorCode.EMAIL_DUPLICATED);
        }
    }

    private void checkNicknameDuplicated(String nickname) {
        if(memberRepository.existsByNickname(nickname)){
            throw new BusinessException(MemberErrorCode.NICKNAME_DUPLICATED);
        }
    }
}
