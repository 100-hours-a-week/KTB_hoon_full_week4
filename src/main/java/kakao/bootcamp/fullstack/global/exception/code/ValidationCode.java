package kakao.bootcamp.fullstack.global.exception.code;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationCode {
    public static final String EMAIL_REQUIRED = "EMAIL_REQUIRED";
    public static final String INVALID_EMAIL_FORMAT = "INVALID_EMAIL_FORMAT";

    public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    public static final String INVALID_PASSWORD_FORMAT = "INVALID_PASSWORD_FORMAT";
    public static final String PASSWORD_CONFIRM_REQUIRED = "PASSWORD_CONFIRM_REQUIRED";

    public static final String NICKNAME_REQUIRED = "NICKNAME_REQUIRED";
    public static final String INVALID_NICKNAME_FORMAT = "INVALID_NICKNAME_FORMAT";

    public static final String IMAGE_REQUIRED = "IMAGE_REQUIRED";
    public static final String INVALID_IMAGE_URL = "INVALID_IMAGE_URL";

    public static final String TITLE_REQUIRED = "TITLE_REQUIRED";
    public static final String CONTENT_REQUIRED = "CONTENT_REQUIRED";

    public static final String COMMENT_REQUIRED = "COMMENT_REQUIRED";

    public static final String REPORT_REASON_REQUIRED = "REPORT_REASON_REQUIRED";
    public static final String REPORT_TARGET_REQUIRED = "REPORT_TARGET_REQUIRED";
}
