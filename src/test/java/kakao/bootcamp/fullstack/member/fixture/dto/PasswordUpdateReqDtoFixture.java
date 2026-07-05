package kakao.bootcamp.fullstack.member.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.PasswordUpdateReqDto;

public class PasswordUpdateReqDtoFixture {

    private static final String DEFAULT_PASSWORD = "newPassword1!";

    public static PasswordUpdateReqDto valid() {
        return new PasswordUpdateReqDto(DEFAULT_PASSWORD, DEFAULT_PASSWORD);
    }

    public static PasswordUpdateReqDto withPasswordConfirm(String password, String passwordConfirm) {
        return new PasswordUpdateReqDto(password, passwordConfirm);
    }

}
