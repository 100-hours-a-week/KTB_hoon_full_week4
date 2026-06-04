package kakao.bootcamp.fullstack.global.response;


import kakao.bootcamp.fullstack.global.exception.code.BaseCode;

public record ApiResponse<T>(
        String message,
        String code,
        T data
) {

    public static <T> ApiResponse<T> success(BaseCode code, T data) {
        return new ApiResponse<>(code.getMessage(), code.getCode(), data);
    }

    public static <T> ApiResponse<T> success(BaseCode code) {
        return new ApiResponse<>(code.getMessage(), code.getCode(), null);
    }

    public static ApiResponse<Void> error(BaseCode code) {
        return new ApiResponse<>(code.getMessage(), code.getCode(), null);
    }
}
