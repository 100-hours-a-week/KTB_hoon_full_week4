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
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.error(e.getMessage(), e);
        BaseCode code = e.getCode();
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException e) {
        log.error(e.getMessage(), e);
        String validationCode =
                e.getAllErrors().stream()
                        .findFirst()
                        .map(MessageSourceResolvable::getDefaultMessage)
                        .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        BaseCode errorCode =
                ErrorCodeMapper.from(validationCode)
                        .orElse(CommonErrorCode.UNMAPPED_VALIDATION_ERROR);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e) {
        log.error(e.getMessage(), e);
        BaseCode bindingFailure = resolveBindingFailure(e.getBindingResult());
        if (bindingFailure != null) {
            return ResponseEntity.status(bindingFailure.getHttpStatus())
                    .body(ApiResponse.error(bindingFailure));
        }
        String validationCode =
                e.getBindingResult().getAllErrors().stream()
                        .findFirst()
                        .map(DefaultMessageSourceResolvable::getDefaultMessage)
                        .orElse(CommonErrorCode.INTERNAL_SERVER_ERROR.getCode());
        BaseCode errorCode =
                ErrorCodeMapper.from(validationCode)
                        .orElse(CommonErrorCode.UNMAPPED_VALIDATION_ERROR);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorCode));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e) {
        log.error(e.getMessage(), e);
        BaseCode code = resolveRequestBodyErrorCode(e.getCause());
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e) {
        log.error(e.getMessage(), e);
        BaseCode code =
                e.getRequiredType() != null && e.getRequiredType().isEnum()
                        ? CommonErrorCode.INVALID_ENUM_VALUE
                        : CommonErrorCode.INVALID_PARAMETER_TYPE;
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(code));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * 값 자체가 타입으로 변환되지 않은 경우(@ModelAttribute 바인딩 실패)는 검증 메시지가 없어 ErrorCodeMapper 로 풀 수 없다. 필드의 실패
     * 코드로 직접 구분한다.
     */
    private BaseCode resolveBindingFailure(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .filter(fieldError -> fieldError.isBindingFailure())
                .findFirst()
                .map(
                        fieldError ->
                                isEnumTarget(bindingResult, fieldError.getField())
                                        ? CommonErrorCode.INVALID_ENUM_VALUE
                                        : CommonErrorCode.INVALID_PARAMETER_TYPE)
                .orElse(null);
    }

    private boolean isEnumTarget(BindingResult bindingResult, String field) {
        Class<?> type = bindingResult.getFieldType(field);
        return type != null && type.isEnum();
    }

    private BaseCode resolveRequestBodyErrorCode(Throwable cause) {
        if (cause instanceof InvalidFormatException ife
                && ife.getTargetType() != null
                && ife.getTargetType().isEnum()) {
            return CommonErrorCode.INVALID_ENUM_VALUE;
        }
        if (cause instanceof MismatchedInputException) {
            return CommonErrorCode.INVALID_REQUEST_BODY;
        }
        return CommonErrorCode.MALFORMED_REQUEST_BODY;
    }
}
