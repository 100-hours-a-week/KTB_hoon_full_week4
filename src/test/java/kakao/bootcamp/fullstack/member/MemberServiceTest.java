package kakao.bootcamp.fullstack.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.service.MemberService;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.PasswordUpdateReqDtoFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.ProfileUpdateReqDtoFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.SignupReqDtoFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MemberServiceTest {

    private static final Long MEMBER_ID = 1L;

    @Mock private MemberRepository memberRepository;

    @Mock private PasswordHasher passwordEncoder;

    @InjectMocks private MemberService memberService;

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("정상적으로 회원가입에 성공한다")
        void signsUpSuccessfully() {
            // given
            SignupReqDto request = SignupReqDtoFixture.valid();
            given(memberRepository.existsByEmail(request.email())).willReturn(false);
            given(memberRepository.existsByNickname(request.nickname())).willReturn(false);
            given(passwordEncoder.hash(request.password())).willReturn("encoded-password");

            // when
            memberService.signup(request);

            // then
            ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
            verify(memberRepository).save(captor.capture());
            Member savedMember = captor.getValue();
            assertThat(savedMember.getEmail()).isEqualTo(request.email());
            assertThat(savedMember.getNickname()).isEqualTo(request.nickname());
            assertThat(savedMember.getEncodedPassword()).isEqualTo("encoded-password");
            assertThat(savedMember.getProfileImgUrl()).isEqualTo(request.imageUrl());
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외를 던진다")
        void throwsExceptionWhenPasswordConfirmMismatch() {
            // given
            SignupReqDto request =
                    SignupReqDtoFixture.withPasswordConfirm("password1!", "different!");

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("이메일이 중복되면 예외를 던진다")
        void throwsExceptionWhenEmailDuplicated() {
            // given
            SignupReqDto request = SignupReqDtoFixture.valid();
            given(memberRepository.existsByEmail(request.email())).willReturn(true);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.EMAIL_DUPLICATED);
            verify(memberRepository, never()).save(any());
        }

        @Test
        @DisplayName("닉네임이 중복되면 예외를 던진다")
        void throwsExceptionWhenNicknameDuplicated() {
            // given
            SignupReqDto request = SignupReqDtoFixture.valid();
            given(memberRepository.existsByEmail(request.email())).willReturn(false);
            given(memberRepository.existsByNickname(request.nickname())).willReturn(true);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
            verify(memberRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMemberProfile()")
    class GetMemberProfile {

        @Test
        @DisplayName("존재하는 회원이면 프로필을 반환한다")
        void returnsProfileWhenMemberExists() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            MemberProfileResDto response = memberService.getMemberProfile(MEMBER_ID);

            // then
            assertThat(response.email()).isEqualTo(member.getEmail());
            assertThat(response.nickname()).isEqualTo(member.getNickname());
            assertThat(response.imageUrl()).isEqualTo(member.getProfileImgUrl());
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> memberService.getMemberProfile(MEMBER_ID))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("deleteMember()")
    class DeleteMember {

        @Test
        @DisplayName("존재하는 회원을 삭제 처리한다")
        void deletesMemberSuccessfully() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            memberService.deleteMember(MEMBER_ID);

            // then
            assertThat(member.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> memberService.deleteMember(MEMBER_ID))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updateMemberProfile()")
    class UpdateMemberProfile {

        @Test
        @DisplayName("닉네임 변경 없이 프로필을 수정한다")
        void updatesProfileWithoutNicknameChange() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            ProfileUpdateReqDto request =
                    ProfileUpdateReqDtoFixture.withNickname(member.getNickname());
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));

            // when
            memberService.updateMemberProfile(MEMBER_ID, request);

            // then
            assertThat(member.getNickname()).isEqualTo(request.nickname());
            assertThat(member.getProfileImgUrl()).isEqualTo(request.imageUrl());
            verify(memberRepository, never()).existsByNickname(any());
        }

        @Test
        @DisplayName("닉네임을 변경하고, 중복이 아니면 정상적으로 수정된다")
        void updatesProfileWithNewNickname() {
            // given
            String newNickname = "new-nick";
            Member member = MemberFixture.activeMember(MEMBER_ID);
            ProfileUpdateReqDto request = ProfileUpdateReqDtoFixture.withNickname(newNickname);
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname(newNickname)).willReturn(false);

            // when
            memberService.updateMemberProfile(MEMBER_ID, request);

            // then
            assertThat(member.getNickname()).isEqualTo(newNickname);
            assertThat(member.getProfileImgUrl()).isEqualTo(request.imageUrl());
        }

        @Test
        @DisplayName("변경하려는 닉네임이 중복이면 예외를 던진다")
        void throwsExceptionWhenNewNicknameDuplicated() {
            // given
            String duplicatedNickname = "duplicated-nick";
            Member member = MemberFixture.activeMember(MEMBER_ID);
            String originalNickname = member.getNickname();
            ProfileUpdateReqDto request =
                    ProfileUpdateReqDtoFixture.withNickname(duplicatedNickname);
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));
            given(memberRepository.existsByNickname(duplicatedNickname)).willReturn(true);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updateMemberProfile(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
            assertThat(member.getNickname()).isEqualTo(originalNickname);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            ProfileUpdateReqDto request = ProfileUpdateReqDtoFixture.valid();
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> memberService.updateMemberProfile(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("정상적으로 비밀번호를 변경한다")
        void updatesPasswordSuccessfully() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            PasswordUpdateReqDto request = PasswordUpdateReqDtoFixture.valid();
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.currentPassword(), member.getEncodedPassword()))
                    .willReturn(true);
            given(passwordEncoder.hash(request.password())).willReturn("new-encoded");

            // when
            memberService.updatePassword(MEMBER_ID, request);

            // then
            verify(passwordEncoder).hash(request.password());
            assertThat(member.getEncodedPassword()).isEqualTo("new-encoded");
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 예외를 던진다")
        void throwsExceptionWhenCurrentPasswordMismatch() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            String originalEncodedPassword = member.getEncodedPassword();
            PasswordUpdateReqDto request =
                    PasswordUpdateReqDtoFixture.withCurrentPassword("wrongCurrent1!");
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.currentPassword(), originalEncodedPassword))
                    .willReturn(false);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.CURRENT_PASSWORD_MISMATCH);
            verify(passwordEncoder, never()).hash(any());
            assertThat(member.getEncodedPassword()).isEqualTo(originalEncodedPassword);
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외를 던진다")
        void throwsExceptionWhenPasswordConfirmMismatch() {
            // given
            Member member = MemberFixture.activeMember(MEMBER_ID);
            String originalEncodedPassword = member.getEncodedPassword();
            PasswordUpdateReqDto request =
                    PasswordUpdateReqDtoFixture.withPasswordConfirm("newPassword1!", "different!");
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.of(member));
            given(passwordEncoder.matches(request.currentPassword(), originalEncodedPassword))
                    .willReturn(true);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
            assertThat(member.getEncodedPassword()).isEqualTo(originalEncodedPassword);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            PasswordUpdateReqDto request = PasswordUpdateReqDtoFixture.valid();
            given(memberRepository.findActiveById(MEMBER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
