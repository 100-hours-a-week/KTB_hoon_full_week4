package kakao.bootcamp.fullstack.global.exception.code;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;

public class ErrorCodeMapper {

    private ErrorCodeMapper() {
    }

    public static Optional<BaseCode> from(String code) {
        return Stream.of(
                        Arrays.stream(MemberErrorCode.values()),
                        Arrays.stream(PostErrorCode.values()),
                        Arrays.stream(CommonErrorCode.values())
                )
                .flatMap(stream -> stream)
                .map(errorCode -> (BaseCode) errorCode)
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst();
    }
}