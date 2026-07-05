package kakao.bootcamp.fullstack.member.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.SignupReqDto;

public class SignupReqDtoFixture {

    private static final String DEFAULT_EMAIL = "test@example.com";
    private static final String DEFAULT_NICKNAME = "nick";
    private static final String DEFAULT_PASSWORD = "password1!";
    private static final String DEFAULT_IMAGE_URL = "url";

    public static SignupReqDto valid() {
        return new SignupReqDto(DEFAULT_EMAIL, DEFAULT_NICKNAME, DEFAULT_PASSWORD, DEFAULT_PASSWORD, DEFAULT_IMAGE_URL);
    }

    public static SignupReqDto withPasswordConfirm(String password, String passwordConfirm) {
        return new SignupReqDto(DEFAULT_EMAIL, DEFAULT_NICKNAME, password, passwordConfirm, DEFAULT_IMAGE_URL);
    }
}