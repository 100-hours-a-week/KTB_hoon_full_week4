package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements BaseCode {

    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "post_not_found"),
    NOT_POST_WRITER(HttpStatus.UNAUTHORIZED, "NOT_POST_WRITER", "not_post_writer"),
    POST_ALREADY_LIKED(HttpStatus.CONFLICT,  "POST_ALREADY_LIKED", "post_already_liked"),
    POST_ALREADY_UNLIKED(HttpStatus.CONFLICT,  "POST_ALREADY_UNLIKED", "post_already_unliked"),
    POST_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "POST_RATE_LIMIT_EXCEEDED", "post_rate_limited"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
