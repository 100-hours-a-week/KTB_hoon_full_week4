package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PostErrorCode implements BaseCode {
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "post_not_found"),
    NOT_POST_WRITER(HttpStatus.FORBIDDEN, "NOT_POST_WRITER", "not_post_writer"),
    POST_ALREADY_LIKED(HttpStatus.CONFLICT, "POST_ALREADY_LIKED", "post_already_liked"),
    POST_ALREADY_UNLIKED(HttpStatus.CONFLICT, "POST_ALREADY_UNLIKED", "post_already_unliked"),
    POST_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS, "POST_RATE_LIMIT_EXCEEDED", "post_rate_limited"),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "INVALID_PAGE_SIZE", "invalid_page_size"),
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "TITLE_REQUIRED", "title_required"),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "CONTENT_REQUIRED", "content_required"),
    POST_IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "POST_IMAGE_REQUIRED", "post_image_required"),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "CATEGORY_REQUIRED", "category_required"),
    MEETING_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "MEETING_TYPE_REQUIRED", "meeting_type_required"),
    ADDRESS_REQUIRED_FOR_OFFLINE(
            HttpStatus.BAD_REQUEST, "ADDRESS_REQUIRED_FOR_OFFLINE", "address_required_for_offline"),
    SIDO_REQUIRED(HttpStatus.BAD_REQUEST, "SIDO_REQUIRED", "sido_required"),
    SIGUNGU_REQUIRED(HttpStatus.BAD_REQUEST, "SIGUNGU_REQUIRED", "sigungu_required"),
    EUPMYEONDONG_REQUIRED(HttpStatus.BAD_REQUEST, "EUPMYEONDONG_REQUIRED", "eupmyeondong_required"),
    PLACE_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "PLACE_NAME_REQUIRED", "place_name_required"),
    PLACE_NAME_LENGTH_EXCEEDED(
            HttpStatus.BAD_REQUEST, "PLACE_NAME_LENGTH_EXCEEDED", "place_name_length_exceeded"),
    CAPACITY_POSITIVE(HttpStatus.BAD_REQUEST, "CAPACITY_POSITIVE", "capacity_positive"),
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
