package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class NotFoundException extends BusinessException{
    public NotFoundException(BaseCode code) {
        super(code);
    }
}
