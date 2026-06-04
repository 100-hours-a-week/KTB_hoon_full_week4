package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import org.hibernate.validator.constraints.URL;

public record SignupReqDto(
        @NotBlank(message = ValidationCode.EMAIL_REQUIRED)
        @Email(message = ValidationCode.INVALID_EMAIL_FORMAT)
        String email,
        @NotBlank(message = ValidationCode.NICKNAME_REQUIRED)
        @Pattern(
                regexp = "^\\S{1,10}$",
                message = ValidationCode.INVALID_NICKNAME_FORMAT
        )
        String nickname,
        @NotBlank(message = ValidationCode.PASSWORD_REQUIRED)
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$",
                message = ValidationCode.INVALID_PASSWORD_FORMAT
        )
        String password,
        @NotBlank(message = ValidationCode.PASSWORD_CONFIRM_REQUIRED)
        String passwordConfirm,
        @URL(message = ValidationCode.INVALID_IMAGE_URL)
        @NotBlank(message = ValidationCode.IMAGE_REQUIRED)
        String imageUrl
) {

}
