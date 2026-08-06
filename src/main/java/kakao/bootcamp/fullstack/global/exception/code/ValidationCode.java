package kakao.bootcamp.fullstack.global.exception.code;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidationCode {
    public static final String EMAIL_REQUIRED = "EMAIL_REQUIRED";
    public static final String INVALID_EMAIL_FORMAT = "INVALID_EMAIL_FORMAT";

    public static final String PASSWORD_REQUIRED = "PASSWORD_REQUIRED";
    public static final String INVALID_PASSWORD_FORMAT = "INVALID_PASSWORD_FORMAT";
    public static final String CURRENT_PASSWORD_REQUIRED = "CURRENT_PASSWORD_REQUIRED";
    public static final String PASSWORD_CONFIRM_REQUIRED = "PASSWORD_CONFIRM_REQUIRED";

    public static final String NICKNAME_REQUIRED = "NICKNAME_REQUIRED";
    public static final String INVALID_NICKNAME_FORMAT = "INVALID_NICKNAME_FORMAT";

    public static final String IMAGE_REQUIRED = "IMAGE_REQUIRED";

    public static final String INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";

    public static final String TITLE_REQUIRED = "TITLE_REQUIRED";
    public static final String TITLE_LENGTH_EXCEEDED = "TITLE_LENGTH_EXCEEDED";
    public static final String CONTENT_REQUIRED = "CONTENT_REQUIRED";

    public static final String CATEGORY_REQUIRED = "CATEGORY_REQUIRED";
    public static final String MEETING_TYPE_REQUIRED = "MEETING_TYPE_REQUIRED";
    public static final String ADDRESS_REQUIRED_FOR_OFFLINE = "ADDRESS_REQUIRED_FOR_OFFLINE";
    public static final String SIDO_REQUIRED = "SIDO_REQUIRED";
    public static final String SIGUNGU_REQUIRED = "SIGUNGU_REQUIRED";
    public static final String EUPMYEONDONG_REQUIRED = "EUPMYEONDONG_REQUIRED";
    public static final String PLACE_NAME_REQUIRED = "PLACE_NAME_REQUIRED";
    public static final String PLACE_NAME_LENGTH_EXCEEDED = "PLACE_NAME_LENGTH_EXCEEDED";
    public static final String CAPACITY_POSITIVE = "CAPACITY_POSITIVE";

    public static final String COMMENT_REQUIRED = "COMMENT_REQUIRED";

    public static final String REPORT_REASON_REQUIRED = "REPORT_REASON_REQUIRED";
    public static final String REPORT_TARGET_REQUIRED = "REPORT_TARGET_REQUIRED";
}
