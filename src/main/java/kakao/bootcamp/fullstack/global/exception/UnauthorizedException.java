package kakao.bootcamp.fullstack.global.exception;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public class UnauthorizedException extends BusinessException {
    public UnauthorizedException(BaseCode code) {
        super(code);
    }
}