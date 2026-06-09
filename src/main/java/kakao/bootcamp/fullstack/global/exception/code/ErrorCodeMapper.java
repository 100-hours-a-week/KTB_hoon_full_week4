package kakao.bootcamp.fullstack.global.exception.code;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import kakao.bootcamp.fullstack.api.domain.member.MemberErrorCode;
import kakao.bootcamp.fullstack.api.domain.comment.CommentErrorCode;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraftErrorCode;
import kakao.bootcamp.fullstack.api.domain.post.PostErrorCode;
import kakao.bootcamp.fullstack.api.domain.report.ReportErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ErrorCodeMapper {

    public static Optional<BaseCode> from(String code) {
        return Stream.of(
                        Arrays.stream(MemberErrorCode.values()),
                        Arrays.stream(PostErrorCode.values()),
                        Arrays.stream(PostDraftErrorCode.values()),
                        Arrays.stream(CommentErrorCode.values()),
                        Arrays.stream(ReportErrorCode.values()),
                        Arrays.stream(CommonErrorCode.values())
                )
                .flatMap(stream -> stream)
                .map(errorCode -> (BaseCode) errorCode)
                .filter(errorCode -> errorCode.getCode().equals(code))
                .findFirst();
    }
}