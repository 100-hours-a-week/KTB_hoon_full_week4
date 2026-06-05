package kakao.bootcamp.fullstack.global.exception.code;


import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements BaseCode{

    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "internal_server_error"
    ),
    INVALID_IMAGE_URL(
            HttpStatus.BAD_REQUEST,
            "INVALID_IMAGE_URL",
            "invalid_image_url"
    )
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public static CommonErrorCode from(String code) {
        return Arrays.stream(values())
                .filter(errorCode -> errorCode.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Not Defined Error Code: " + code));
    }
}
