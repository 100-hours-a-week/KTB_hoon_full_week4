package kakao.bootcamp.fullstack.api.domain.post;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MeetingType {
    ONLINE("온라인"),
    OFFLINE("오프라인");

    private final String label;
}
