package kakao.bootcamp.fullstack.api.domain.report;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseCode {

    REPORT_TARGET_REQUIRED(HttpStatus.BAD_REQUEST, "REPORT_TARGET_REQUIRED", "report_target_required"),
    REPORT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "REPORT_REASON_REQUIRED", "report_reason_required"),
    SELF_REPORT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SELF_REPORT_NOT_ALLOWED", "self_report_not_allowed"),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "ALREADY_REPORTED", "already_reported"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
