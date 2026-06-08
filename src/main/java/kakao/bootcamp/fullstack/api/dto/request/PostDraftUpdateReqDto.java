package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.Size;
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record PostDraftUpdateReqDto(
        @Size(max = PostConstants.TITLE_MAX_LENGTH, message = ValidationCode.TITLE_LENGTH_EXCEEDED)
        String title,
        String content,
        String imageUrl
) {}
