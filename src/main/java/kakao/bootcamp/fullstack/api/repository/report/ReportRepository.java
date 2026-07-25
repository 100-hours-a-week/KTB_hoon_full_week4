package kakao.bootcamp.fullstack.api.repository.report;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.report.Report;

public interface ReportRepository {

    void save(Report report);

    boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId);
}
