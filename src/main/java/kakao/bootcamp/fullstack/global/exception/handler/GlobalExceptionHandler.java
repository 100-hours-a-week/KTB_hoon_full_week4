package kakao.bootcamp.fullstack.global.exception.handler;

import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import kakao.bootcamp.fullstack.global.exception.code.ErrorCodeMapper;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn(e.getMessage(), e);
        BaseCode code = e.getCode();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn(e.getMessage(), e);
        String validationCode = e.getBindingResult()
                .getAllErrors()
                .stream()
                .findFirst()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        BaseCode errorCode = ErrorCodeMapper.from(validationCode)
                .orElse(CommonErrorCode.UNMAPPED_VALIDATION_ERROR);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity
                .status(CommonErrorCode.INVALID_ENUM_VALUE.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_ENUM_VALUE));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
