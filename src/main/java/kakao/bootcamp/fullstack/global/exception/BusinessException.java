package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseCode code;

    public BusinessException(BaseCode code) {
        super(code.getMessage());
        this.code = code;
    }

    public BusinessException(BaseCode code, String message) {
        super(message);
        this.code = code;
    }
}