package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class ForbiddenException extends BusinessException {
    public ForbiddenException(BaseCode code) {
        super(code);
    }
}
