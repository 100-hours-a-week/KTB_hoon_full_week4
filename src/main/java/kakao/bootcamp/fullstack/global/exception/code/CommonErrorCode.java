package kakao.bootcamp.fullstack.global.exception.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseCode{

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "internal_server_error"),
    ALREADY_ASSIGNED_ID(HttpStatus.INTERNAL_SERVER_ERROR, "ALREADY_ASSIGNED_ID", "already_assigned_id"),
    UNMAPPED_VALIDATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "UNMAPPED_VALIDATION_ERROR", "unmapped_validation_error"),
    HANDLER_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "HANDLER_NOT_FOUND", "handler_not_found"),
    INVALID_ENUM_VALUE(HttpStatus.BAD_REQUEST, "INVALID_ENUM_VALUE", "invalid_enum_value"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
