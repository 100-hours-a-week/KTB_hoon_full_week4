package kakao.bootcamp.fullstack.api.domain.post;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecruitStatus {
    RECRUITING("모집중"),
    CLOSED("모집완료");

    private final String label;
}
