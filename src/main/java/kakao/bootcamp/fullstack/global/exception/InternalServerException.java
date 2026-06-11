package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class InternalServerException extends BusinessException {

    public InternalServerException(BaseCode code) {
        super(code);
    }

    public InternalServerException(BaseCode code, String message) {
        super(code, message);
    }
}
