package kakao.bootcamp.fullstack.api.repository.report;

import kakao.bootcamp.fullstack.api.domain.report.Report;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;

public interface ReportRepository {

    void save(Report report);

    boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId);
}
