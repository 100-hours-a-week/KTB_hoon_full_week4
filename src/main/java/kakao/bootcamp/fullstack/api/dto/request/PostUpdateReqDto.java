package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.global.constants.PostConstants;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record PostUpdateReqDto(
        @NotEmpty(message = ValidationCode.TITLE_REQUIRED)
                @Size(
                        max = PostConstants.TITLE_MAX_LENGTH,
                        message = ValidationCode.TITLE_LENGTH_EXCEEDED)
                String title,
        @NotEmpty(message = ValidationCode.CONTENT_REQUIRED) String content,
        @NotEmpty(message = ValidationCode.IMAGE_REQUIRED) String imageUrl,
        @NotNull(message = ValidationCode.CATEGORY_REQUIRED) PostCategory category,
        @NotNull(message = ValidationCode.MEETING_TYPE_REQUIRED) MeetingType meetingType,
        @Valid AddressReqDto address,
        @NotBlank(message = ValidationCode.PLACE_NAME_REQUIRED)
                @Size(
                        max = PostConstants.PLACE_NAME_MAX_LENGTH,
                        message = ValidationCode.PLACE_NAME_LENGTH_EXCEEDED)
                String placeName,
        @Positive(message = ValidationCode.CAPACITY_POSITIVE) Integer capacity) {

    @AssertTrue(message = ValidationCode.ADDRESS_REQUIRED_FOR_OFFLINE)
    public boolean isAddressPresentWhenOffline() {
        if (meetingType != MeetingType.OFFLINE) {
            return true;
        }
        return address != null;
    }
}
