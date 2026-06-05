package kakao.bootcamp.fullstack.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode{
    SIGNUP_SUCCESS(HttpStatus.CREATED, "SUCCESS", "signup_success"),
    LOGIN_SUCCESS(HttpStatus.OK, "SUCCESS", "login_success"),
    LOGOUT_SUCCESS(HttpStatus.OK, "SUCCESS", "logout_success"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
