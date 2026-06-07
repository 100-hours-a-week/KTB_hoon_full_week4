package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotEmpty;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record CommentUpdateReqDto(
        @NotEmpty(message = ValidationCode.COMMENT_REQUIRED)
        String content
) {}
