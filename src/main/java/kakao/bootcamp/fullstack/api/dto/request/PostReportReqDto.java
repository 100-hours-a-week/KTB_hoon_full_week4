package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotNull;
import kakao.bootcamp.fullstack.api.domain.report.ReportReason;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record PostReportReqDto(
        @NotNull(message = ValidationCode.REPORT_TARGET_REQUIRED)
        Long targetId,
        @NotNull(message = ValidationCode.REPORT_TARGET_REQUIRED)
        TargetType targetType,
        @NotNull(message = ValidationCode.REPORT_REASON_REQUIRED)
        ReportReason reportReason
) {
}
