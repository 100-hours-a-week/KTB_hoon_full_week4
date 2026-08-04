package kakao.bootcamp.fullstack.member.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;

public class SignupReqDtoFixture {

    public static final String EMAIL = "test@example.com";
    public static final String NICKNAME = "nick";
    public static final String PASSWORD = "password1!";
    public static final String IMAGE_URL = "url";

    public static SignupReqDto valid() {
        return new SignupReqDto(EMAIL, NICKNAME, PASSWORD, PASSWORD, IMAGE_URL);
    }

    public static SignupReqDto withPasswordConfirm(String password, String passwordConfirm) {
        return new SignupReqDto(EMAIL, NICKNAME, password, passwordConfirm, IMAGE_URL);
    }
}
