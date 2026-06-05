package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record LoginReqDto(
        @NotBlank(message = ValidationCode.EMAIL_REQUIRED)
        @Email(message = ValidationCode.INVALID_EMAIL_FORMAT)
        String email,

        @NotBlank(message = ValidationCode.PASSWORD_REQUIRED)
        String password
) {
}