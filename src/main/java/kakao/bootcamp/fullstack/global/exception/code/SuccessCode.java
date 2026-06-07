package kakao.bootcamp.fullstack.global.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SuccessCode implements BaseCode{
    SUCCESS(HttpStatus.OK, "SUCCESS", "success"),
    CREATED(HttpStatus.CREATED, "SUCCESS", "created"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
