package kakao.bootcamp.fullstack.api.domain.auth;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {
    MEMBER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "MEMBER_NOT_FOUND", "member_not_found"),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "PASSWORD_MISMATCH", "password_mismatch"),
    TOKEN_EMPTY(HttpStatus.UNAUTHORIZED, "TOKEN_EMPTY", "token_empty"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "invalid_token"),
    TOKEN_BLACKLISTED(HttpStatus.UNAUTHORIZED, "TOKEN_BLACKLISTED", "token_blacklisted"),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
