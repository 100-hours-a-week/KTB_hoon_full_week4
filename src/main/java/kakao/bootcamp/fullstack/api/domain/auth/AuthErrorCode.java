package kakao.bootcamp.fullstack.api.domain.auth;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "login_failed"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "invalid_token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "access_denied"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
