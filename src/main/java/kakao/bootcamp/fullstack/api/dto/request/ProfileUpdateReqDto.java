package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.jwt.annotation.ValidNickname;
import org.hibernate.validator.constraints.URL;

public record ProfileUpdateReqDto(
        @NotBlank(message = ValidationCode.NICKNAME_REQUIRED)
        @ValidNickname(message = ValidationCode.INVALID_NICKNAME_FORMAT)
        String nickname,
        @NotBlank(message = ValidationCode.INVALID_IMAGE_URL)
        @URL(message = ValidationCode.IMAGE_REQUIRED)
        String imageUrl
) {

}
