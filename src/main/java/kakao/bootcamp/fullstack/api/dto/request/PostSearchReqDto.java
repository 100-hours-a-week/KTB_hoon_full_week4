package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import org.springframework.format.annotation.DateTimeFormat;

public record PostSearchReqDto(
        String keyword,
        PostCategory category,
        MeetingType meetingType,
        RecruitStatus recruitStatus,
        String sido,
        String sigungu,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        Long cursor,
        @Min(value = 1, message = ValidationCode.INVALID_PAGE_SIZE)
                @Max(value = 10, message = ValidationCode.INVALID_PAGE_SIZE)
                Long size) {

    public PostSearchReqDto {
        size = size == null ? 10L : size;
    }

    @AssertTrue(message = ValidationCode.INVALID_DATE_RANGE)
    public boolean isFromNotAfterTo() {
        return from == null || to == null || !from.isAfter(to);
    }
}
