package kakao.bootcamp.fullstack.api.domain.auth;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "login_failed"),
    TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, "TOKEN_EMPTY", "token_empty"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "invalid_token"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "token_blacklisted"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN", "expired_token"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "access_denied"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
