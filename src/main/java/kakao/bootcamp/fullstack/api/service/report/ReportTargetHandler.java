package kakao.bootcamp.fullstack.api.service.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;

public interface ReportTargetHandler {

    TargetType getTargetType();

    /** 신고 대상이 존재하지 않으면 도메인별 NOT_FOUND 예외를 던진다. */
    boolean isWrittenBy(Long targetId, Long memberId);

    void handleReported(Long targetId);
}
