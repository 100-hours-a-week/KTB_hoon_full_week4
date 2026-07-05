package kakao.bootcamp.fullstack.member;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class MemberTest {

    private static final String EMAIL = "test@example.com";
    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String NICKNAME = "tester";
    private static final String PROFILE_IMG_URL = "https://example.com/profile.png";

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("필드 값을 그대로 반영하여 Member를 생성한다")
        void createsMemberWithGivenFields() {
            // when
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);

            // then
            assertThat(member.getEmail()).isEqualTo(EMAIL);
            assertThat(member.getEncodedPassword()).isEqualTo(ENCODED_PASSWORD);
            assertThat(member.getNickname()).isEqualTo(NICKNAME);
            assertThat(member.getProfileImgUrl()).isEqualTo(PROFILE_IMG_URL);
        }

        @Test
        @DisplayName("기본 권한은 ROLE_USER이다")
        void defaultRoleIsUser() {
            // when
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);

            // then
            assertThat(member.getRole()).isEqualTo(Role.ROLE_USER);
        }

        @Test
        @DisplayName("id가 없는 상태로 생성된다 (isNew = true)")
        void createdMemberIsNew() {
            // when
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);

            // then
            assertThat(member.isNew()).isTrue();
        }
    }

    @Nested
    @DisplayName("assignId()")
    class AssignId {

        @Test
        @DisplayName("id가 없는 경우 정상적으로 할당된다")
        void assignsIdWhenNew() {
            // given
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);

            // when
            member.assignId(1L);

            // then
            assertThat(member.getId()).isEqualTo(1L);
            assertThat(member.isNew()).isFalse();
        }

        @Test
        @DisplayName("id가 이미 있는 경우 예외를 던진다")
        void throwsExceptionWhenAlreadyAssigned() {
            // given
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);
            member.assignId(1L);

            // when & then
            assertThatThrownBy(() -> member.assignId(2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", CommonErrorCode.ALREADY_ASSIGNED_ID);
        }
    }

    @Nested
    @DisplayName("updateProfile()")
    class UpdateProfile {

        @Test
        @DisplayName("nickname과 profileImgUrl을 변경한다")
        void updatesNicknameAndProfileImgUrl() {
            // given
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);
            String newNickname = "new-nickname";
            String newProfileImgUrl = "https://example.com/new-profile.png";

            // when
            member.updateProfile(newNickname, newProfileImgUrl);

            // then
            assertThat(member.getNickname()).isEqualTo(newNickname);
            assertThat(member.getProfileImgUrl()).isEqualTo(newProfileImgUrl);
        }
    }

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("encodedPassword를 변경한다")
        void updatesEncodedPassword() {
            // given
            Member member = Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);
            String newEncodedPassword = "new-encoded-password";

            // when
            member.updatePassword(newEncodedPassword);

            // then
            assertThat(member.getEncodedPassword()).isEqualTo(newEncodedPassword);
        }
    }
}
