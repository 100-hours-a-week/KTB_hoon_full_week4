package kakao.bootcamp.fullstack.api.domain.post_draft;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum PostDraftErrorCode implements BaseCode {

    POST_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_DRAFT_NOT_FOUND", "post_draft_not_found"),
    NOT_POST_DRAFT_WRITER(HttpStatus.UNAUTHORIZED, "NOT_POST_DRAFT_WRITER", "not_post_draft_writer"),
    TITLE_LENGTH_EXCEEDED(HttpStatus.BAD_REQUEST, "TITLE_LENGTH_EXCEEDED", "title_length_exceeded"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
