package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import org.hibernate.validator.constraints.URL;

public record PostCreateReqDto(
        @NotEmpty(message = ValidationCode.TITLE_REQUIRED)
        String title,
        @NotEmpty(message = ValidationCode.CONTENT_REQUIRED)
        String content,
        @NotEmpty(message = ValidationCode.IMAGE_REQUIRED)
        @URL(message = ValidationCode.INVALID_IMAGE_URL)
        String imageUrl
) {}