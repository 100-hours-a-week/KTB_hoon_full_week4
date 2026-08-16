package kakao.bootcamp.fullstack.api.domain.search;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements BaseCode {
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "INVALID_DATE_RANGE", "invalid_date_range"),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "invalid_cursor"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
