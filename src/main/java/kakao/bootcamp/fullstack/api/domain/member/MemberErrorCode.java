package kakao.bootcamp.fullstack.api.domain.member;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements BaseCode {

    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_EMAIL_FORMAT", "invalid_email_format"),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_PASSWORD_FORMAT", "invalid_pw_format"),
    INVALID_NICKNAME_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME_FORMAT", "invalid_nickname_format"),

    EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "email_required"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "PASSWORD_REQUIRED", "pw_required"),
    NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "NICKNAME_REQUIRED", "nickname_required"),
    PROFILE_IMG_REQUIRED(HttpStatus.BAD_REQUEST, "PROFILE_IMG_REQUIRED", "profile_img_required"),

    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "pw_confirm_mismatch"),
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "EMAIL_DUPLICATED", "email_duplicated"),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "NICKNAME_DUPLICATED", "nickname_duplicated");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}