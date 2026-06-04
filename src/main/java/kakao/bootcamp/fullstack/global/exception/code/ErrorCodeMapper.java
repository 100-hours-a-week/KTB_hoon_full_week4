package kakao.bootcamp.fullstack.global.exception.code;

import java.util.Arrays;
import java.util.stream.Stream;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;

public final class ErrorCodeMapper {

    private ErrorCodeMapper() {
    }

    public static BaseCode from(String code) {
        return Stream.of(
                        MemberErrorCode.values(),
                        PostErrorCode.values()
                )
                .flatMap(Arrays::stream)
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("정의되지 않은 ValidationCode입니다: " + code)
                );
    }
}