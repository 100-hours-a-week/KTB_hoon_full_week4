package kakao.bootcamp.fullstack.global.exception.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.BaseCode;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import kakao.bootcamp.fullstack.global.exception.code.ErrorCodeMapper;
import kakao.bootcamp.fullstack.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

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

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException e) {
        log.warn(e.getMessage(), e);
        String validationCode = e.getAllErrors().stream()
                .findFirst()
                .map(MessageSourceResolvable::getDefaultMessage)
                .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        BaseCode errorCode = ErrorCodeMapper.from(validationCode)
                .orElse(CommonErrorCode.UNMAPPED_VALIDATION_ERROR);

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode));
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
        BaseCode code = resolveRequestBodyErrorCode(e.getCause());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.warn(e.getMessage(), e);
        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    private BaseCode resolveRequestBodyErrorCode(Throwable cause) {
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            return CommonErrorCode.INVALID_ENUM_VALUE;
        }
        if (cause instanceof MismatchedInputException) {
            return CommonErrorCode.INVALID_REQUEST_BODY;
        }
        return CommonErrorCode.MALFORMED_REQUEST_BODY;
    }
}
