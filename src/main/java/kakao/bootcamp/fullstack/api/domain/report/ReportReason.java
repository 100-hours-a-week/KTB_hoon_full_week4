package kakao.bootcamp.fullstack.api.domain.report;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportReason {
    SPAM(0, "스팸"),
    ABUSE(1, "욕설/비하"),
    INAPPROPRIATE(2, "부적절한 콘텐츠"),
    ADVERTISEMENT(3, "광고"),
    ETC(4, "기타"),
    ;

    private final int code;
    private final String label;

    // TODO : 예외처리 + Optional 처리 필요
    public static Optional<ReportReason> from(int code) {
        return Arrays.stream(values())
                .filter(r -> r.code == code)
                .findFirst();
    }
}