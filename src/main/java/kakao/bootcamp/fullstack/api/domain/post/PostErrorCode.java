package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import org.springframework.http.HttpStatus;

public enum PostErrorCode implements BaseCode {
    ;

    @Override
    public HttpStatus getHttpStatus() {
        return null;
    }

    @Override
    public String getCode() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }
}
