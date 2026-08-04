package kakao.bootcamp.fullstack.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;
import kakao.bootcamp.fullstack.api.dto.response.MemberProfileResDto;
import kakao.bootcamp.fullstack.api.service.MemberService;
import kakao.bootcamp.fullstack.auth.fake.FakePasswordHasher;
import kakao.bootcamp.fullstack.auth.fake.FakeRefreshTokenRepository;
import kakao.bootcamp.fullstack.auth.fixture.RefreshTokenFixture;
import kakao.bootcamp.fullstack.global.exception.BadRequestException;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.NotFoundException;
import kakao.bootcamp.fullstack.member.fake.FakeMemberRepository;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.PasswordUpdateReqDtoFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.ProfileUpdateReqDtoFixture;
import kakao.bootcamp.fullstack.member.fixture.dto.SignupReqDtoFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MemberServiceTest {

    private static final Long MEMBER_ID = RefreshTokenFixture.MEMBER_ID;
    private static final Long OTHER_MEMBER_ID = 2L;
    private static final String NEW_NICKNAME = "new-nick";
    private static final String DUPLICATED_NICKNAME = "duplicated-nick";
    private static final String OTHER_EMAIL = "other@example.com";

    private FakeMemberRepository memberRepository;
    private FakeRefreshTokenRepository refreshTokenRepository;
    private FakePasswordHasher passwordHasher;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = new FakeMemberRepository();
        refreshTokenRepository = new FakeRefreshTokenRepository();
        passwordHasher = new FakePasswordHasher();
        memberService = new MemberService(memberRepository, refreshTokenRepository, passwordHasher);
    }

    @Nested
    @DisplayName("signup()")
    class Signup {

        @Test
        @DisplayName("정상적으로 회원가입하면 해싱된 비밀번호로 회원을 저장한다")
        void signsUpSuccessfully() {
            // when
            memberService.signup(SignupReqDtoFixture.valid());

            // then
            Member saved =
                    memberRepository.findActiveByEmail(SignupReqDtoFixture.EMAIL).orElseThrow();
            assertThat(saved.getEmail()).isEqualTo(SignupReqDtoFixture.EMAIL);
            assertThat(saved.getNickname()).isEqualTo(SignupReqDtoFixture.NICKNAME);
            assertThat(saved.getProfileImgUrl()).isEqualTo(SignupReqDtoFixture.IMAGE_URL);
            assertThat(saved.getEncodedPassword())
                    .isEqualTo(passwordHasher.hash(SignupReqDtoFixture.PASSWORD));
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외를 던지고 저장하지 않는다")
        void throwsExceptionWhenPasswordConfirmMismatch() {
            // given
            var request = SignupReqDtoFixture.withPasswordConfirm("password1!", "different!");

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
            assertThat(memberRepository.findActiveByEmail(SignupReqDtoFixture.EMAIL)).isEmpty();
        }

        @Test
        @DisplayName("이메일이 중복되면 예외를 던진다")
        void throwsExceptionWhenEmailDuplicated() {
            // given
            memberRepository.save(MemberFixture.activeMember(OTHER_MEMBER_ID));

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(SignupReqDtoFixture.valid()))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.EMAIL_DUPLICATED);
        }

        @Test
        @DisplayName("닉네임이 중복되면 예외를 던진다")
        void throwsExceptionWhenNicknameDuplicated() {
            // given
            memberRepository.save(
                    MemberFixture.withEmailAndNickname(
                            OTHER_MEMBER_ID, OTHER_EMAIL, SignupReqDtoFixture.NICKNAME));

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.signup(SignupReqDtoFixture.valid()))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
        }
    }

    @Nested
    @DisplayName("getMemberProfile()")
    class GetMemberProfile {

        @Test
        @DisplayName("존재하는 회원이면 프로필을 반환한다")
        void returnsProfileWhenMemberExists() {
            // given
            memberRepository.save(MemberFixture.activeMember(MEMBER_ID));

            // when
            MemberProfileResDto response = memberService.getMemberProfile(MEMBER_ID);

            // then
            assertThat(response.email()).isEqualTo(MemberFixture.EMAIL);
            assertThat(response.nickname()).isEqualTo(MemberFixture.NICKNAME);
            assertThat(response.imageUrl()).isEqualTo(MemberFixture.PROFILE_IMG_URL);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
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
        @DisplayName("존재하는 회원을 삭제하면 더 이상 조회되지 않는다")
        void deletesMemberSuccessfully() {
            // given
            memberRepository.save(MemberFixture.activeMember(MEMBER_ID));

            // when
            memberService.deleteMember(MEMBER_ID);

            // then
            assertThat(memberRepository.findActiveById(MEMBER_ID)).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
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
            memberRepository.save(MemberFixture.activeMember(MEMBER_ID));
            // 같은 닉네임이 저장소에 이미 있으므로(자기 자신), 중복 검사를 건너뛰지 않으면 오탐으로 실패한다
            ProfileUpdateReqDto request =
                    ProfileUpdateReqDtoFixture.withNickname(MemberFixture.NICKNAME);

            // when
            memberService.updateMemberProfile(MEMBER_ID, request);

            // then
            Member updated = memberRepository.findActiveById(MEMBER_ID).orElseThrow();
            assertThat(updated.getNickname()).isEqualTo(MemberFixture.NICKNAME);
            assertThat(updated.getProfileImgUrl()).isEqualTo(request.imageUrl());
        }

        @Test
        @DisplayName("닉네임을 변경하고, 중복이 아니면 정상적으로 수정된다")
        void updatesProfileWithNewNickname() {
            // given
            memberRepository.save(MemberFixture.activeMember(MEMBER_ID));
            ProfileUpdateReqDto request = ProfileUpdateReqDtoFixture.withNickname(NEW_NICKNAME);

            // when
            memberService.updateMemberProfile(MEMBER_ID, request);

            // then
            Member updated = memberRepository.findActiveById(MEMBER_ID).orElseThrow();
            assertThat(updated.getNickname()).isEqualTo(NEW_NICKNAME);
            assertThat(updated.getProfileImgUrl()).isEqualTo(request.imageUrl());
        }

        @Test
        @DisplayName("변경하려는 닉네임이 중복이면 예외를 던지고 기존 닉네임을 유지한다")
        void throwsExceptionWhenNewNicknameDuplicated() {
            // given
            memberRepository.save(MemberFixture.activeMember(MEMBER_ID));
            memberRepository.save(
                    MemberFixture.withEmailAndNickname(
                            OTHER_MEMBER_ID, OTHER_EMAIL, DUPLICATED_NICKNAME));
            ProfileUpdateReqDto request =
                    ProfileUpdateReqDtoFixture.withNickname(DUPLICATED_NICKNAME);

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updateMemberProfile(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.NICKNAME_DUPLICATED);
            assertThat(memberRepository.findActiveById(MEMBER_ID).orElseThrow().getNickname())
                    .isEqualTo(MemberFixture.NICKNAME);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            ProfileUpdateReqDto request = ProfileUpdateReqDtoFixture.valid();

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
        @DisplayName("정상적으로 비밀번호를 변경하고 해당 회원의 RT를 전부 폐기한다")
        void updatesPasswordSuccessfully() {
            // given
            memberRepository.save(
                    MemberFixture.withEncodedPassword(
                            MEMBER_ID, PasswordUpdateReqDtoFixture.CURRENT_PASSWORD));
            RefreshToken refreshToken = RefreshTokenFixture.active("family-1", "hash-1");
            refreshTokenRepository.save(refreshToken);
            PasswordUpdateReqDto request = PasswordUpdateReqDtoFixture.valid();

            // when
            memberService.updatePassword(MEMBER_ID, request);

            // then
            Member updated = memberRepository.findActiveById(MEMBER_ID).orElseThrow();
            assertThat(updated.getEncodedPassword())
                    .isEqualTo(passwordHasher.hash(PasswordUpdateReqDtoFixture.NEW_PASSWORD));
            assertThat(refreshTokenRepository.findNotDeletedByTokenHash("hash-1").orElseThrow())
                    .extracting(RefreshToken::isRevoked)
                    .isEqualTo(true);
        }

        @Test
        @DisplayName("현재 비밀번호가 일치하지 않으면 예외를 던지고 비밀번호를 바꾸지 않는다")
        void throwsExceptionWhenCurrentPasswordMismatch() {
            // given
            memberRepository.save(
                    MemberFixture.withEncodedPassword(
                            MEMBER_ID, PasswordUpdateReqDtoFixture.CURRENT_PASSWORD));
            PasswordUpdateReqDto request =
                    PasswordUpdateReqDtoFixture.withCurrentPassword("wrongCurrent1!");

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.CURRENT_PASSWORD_MISMATCH);
            assertThat(
                            memberRepository
                                    .findActiveById(MEMBER_ID)
                                    .orElseThrow()
                                    .getEncodedPassword())
                    .isEqualTo(PasswordUpdateReqDtoFixture.CURRENT_PASSWORD);
        }

        @Test
        @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외를 던지고 비밀번호를 바꾸지 않는다")
        void throwsExceptionWhenPasswordConfirmMismatch() {
            // given
            memberRepository.save(
                    MemberFixture.withEncodedPassword(
                            MEMBER_ID, PasswordUpdateReqDtoFixture.CURRENT_PASSWORD));
            PasswordUpdateReqDto request =
                    PasswordUpdateReqDtoFixture.withPasswordConfirm("newPassword1!", "different!");

            // when & then
            assertThatExceptionOfType(BadRequestException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.PASSWORD_CONFIRM_MISMATCH);
            assertThat(
                            memberRepository
                                    .findActiveById(MEMBER_ID)
                                    .orElseThrow()
                                    .getEncodedPassword())
                    .isEqualTo(PasswordUpdateReqDtoFixture.CURRENT_PASSWORD);
        }

        @Test
        @DisplayName("존재하지 않는 회원이면 예외를 던진다")
        void throwsExceptionWhenMemberNotFound() {
            // given
            PasswordUpdateReqDto request = PasswordUpdateReqDtoFixture.valid();

            // when & then
            assertThatExceptionOfType(NotFoundException.class)
                    .isThrownBy(() -> memberService.updatePassword(MEMBER_ID, request))
                    .extracting(BusinessException::getCode)
                    .isEqualTo(MemberErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
