package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class BadRequestException extends BusinessException{

    public BadRequestException(BaseCode code) {
        super(code);
    }
}
