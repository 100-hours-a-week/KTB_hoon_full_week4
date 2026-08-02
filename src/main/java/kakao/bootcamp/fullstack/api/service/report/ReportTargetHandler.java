package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;

public interface ReportTargetHandler {

    TargetType getTargetType();

    void handleReported(Long targetId);
}
