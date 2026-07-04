package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.ValidPassword;

public record PasswordUpdateReqDto(
        @NotBlank(message = ValidationCode.PASSWORD_REQUIRED)
        @ValidPassword(message = ValidationCode.INVALID_PASSWORD_FORMAT)
        String password,
        @NotBlank(message = ValidationCode.PASSWORD_CONFIRM_REQUIRED)
        String passwordConfirm
) {

}
