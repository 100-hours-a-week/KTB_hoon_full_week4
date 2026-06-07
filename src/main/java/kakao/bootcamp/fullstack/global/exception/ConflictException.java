package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class ConflictException extends BusinessException{

    public ConflictException(BaseCode code) {
        super(code);
    }
}
