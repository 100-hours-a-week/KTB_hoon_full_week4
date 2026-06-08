package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class TooManyRequestsException extends BusinessException{

    public TooManyRequestsException(BaseCode code) {
        super(code);
    }
}
