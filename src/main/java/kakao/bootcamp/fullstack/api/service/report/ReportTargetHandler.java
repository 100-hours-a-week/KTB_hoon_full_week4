package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;

public interface ReportTargetHandler {

    boolean supports(TargetType targetType);

    void handleReported(Long targetId);
}
