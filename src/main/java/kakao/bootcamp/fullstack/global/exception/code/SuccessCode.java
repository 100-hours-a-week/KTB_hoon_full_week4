package kakao.bootcamp.fullstack.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode{
    SUCCESS(HttpStatus.OK, "SUCCESS", "success"),
    CREATED(HttpStatus.CREATED, "SUCCESS", "created"),
    COMMENT_CREATED_SUCCESS(HttpStatus.CREATED, "SUCCESS", "comment_created_success"),
    COMMENT_UPDATED_SUCCESS(HttpStatus.OK, "SUCCESS", "comment_updated_success"),
    COMMENT_DELETED_SUCCESS(HttpStatus.OK, "SUCCESS", "comment_deleted_success"),
    REPORT_CREATED_SUCCESS(HttpStatus.CREATED, "SUCCESS", "report_created_success"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
