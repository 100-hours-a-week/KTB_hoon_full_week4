package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.jwt.annotation.ValidNickname;

public record ProfileUpdateReqDto(
        @NotBlank(message = ValidationCode.NICKNAME_REQUIRED)
        @ValidNickname(message = ValidationCode.INVALID_NICKNAME_FORMAT)
        String nickname,
        @NotBlank(message = ValidationCode.IMAGE_REQUIRED)
        String imageUrl
) {

}
