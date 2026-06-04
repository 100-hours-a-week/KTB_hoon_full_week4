package kakao.bootcamp.fullstack.api.service;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.repository.MemberRepository;
import kakao.bootcamp.fullstack.global.config.PasswordHasher;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordHasher passwordHasher;

    public void signup(SignupReqDto request){
        validatePasswordConfirmMatch(request);
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

    private void validatePasswordConfirmMatch(SignupReqDto request) {
        if(!request.password().equals(request.passwordConfirm())){
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
