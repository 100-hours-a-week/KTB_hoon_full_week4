package kakao.bootcamp.fullstack.global.exception.code;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
