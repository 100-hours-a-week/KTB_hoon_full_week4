package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.ValidNickname;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.ValidPassword;

public record SignupReqDto(
        @NotBlank(message = ValidationCode.EMAIL_REQUIRED)
        @Email(message = ValidationCode.INVALID_EMAIL_FORMAT)
        String email,
        @NotBlank(message = ValidationCode.NICKNAME_REQUIRED)
        @ValidNickname(message = ValidationCode.INVALID_NICKNAME_FORMAT)
        String nickname,
        @NotBlank(message = ValidationCode.PASSWORD_REQUIRED)
        @ValidPassword(message = ValidationCode.INVALID_PASSWORD_FORMAT)
        String password,
        @NotBlank(message = ValidationCode.PASSWORD_CONFIRM_REQUIRED)
        String passwordConfirm,
        @NotBlank(message = ValidationCode.IMAGE_REQUIRED)
        String imageUrl
) {

}
