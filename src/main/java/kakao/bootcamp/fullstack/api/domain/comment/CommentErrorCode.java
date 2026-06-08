package kakao.bootcamp.fullstack.api.domain.comment;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements BaseCode {

    COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "COMMENT_REQUIRED", "comment_required"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "comment_not_found"),
    NOT_COMMENT_WRITER(HttpStatus.FORBIDDEN, "NOT_COMMENT_WRITER", "not_comment_writer"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
