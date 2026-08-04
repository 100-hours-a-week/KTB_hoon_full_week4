package kakao.bootcamp.fullstack.member.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;

public class PasswordUpdateReqDtoFixture {

    public static final String CURRENT_PASSWORD = "currentPassword1!";
    public static final String NEW_PASSWORD = "newPassword1!";

    public static PasswordUpdateReqDto valid() {
        return new PasswordUpdateReqDto(CURRENT_PASSWORD, NEW_PASSWORD, NEW_PASSWORD);
    }

    public static PasswordUpdateReqDto withCurrentPassword(String currentPassword) {
        return new PasswordUpdateReqDto(currentPassword, NEW_PASSWORD, NEW_PASSWORD);
    }

    public static PasswordUpdateReqDto withPasswordConfirm(
            String password, String passwordConfirm) {
        return new PasswordUpdateReqDto(CURRENT_PASSWORD, password, passwordConfirm);
    }
}
