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
    PROFILE_GET_SUCCESS(HttpStatus.OK, "SUCCESS", "profile_search_success"),
    PROFILE_UPDATE_SUCCESS(HttpStatus.OK, "SUCCESS", "profile_update_success"),
    PASSWORD_UPDATE_SUCCESS(HttpStatus.OK, "SUCCESS", "password_update_success"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
