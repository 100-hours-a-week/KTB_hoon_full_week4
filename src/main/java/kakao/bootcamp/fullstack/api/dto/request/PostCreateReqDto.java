package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import org.hibernate.validator.constraints.URL;

public record PostCreateReqDto(
        @NotEmpty(message = ValidationCode.TITLE_REQUIRED)
        @Size(max = PostConstants.TITLE_MAX_LENGTH, message = ValidationCode.TITLE_LENGTH_EXCEEDED)
        String title,
        @NotEmpty(message = ValidationCode.CONTENT_REQUIRED)
        String content,
        @NotEmpty(message = ValidationCode.IMAGE_REQUIRED)
        @URL(message = ValidationCode.INVALID_IMAGE_URL)
        String imageUrl
) {}