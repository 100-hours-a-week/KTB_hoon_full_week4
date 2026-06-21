package kakao.bootcamp.fullstack.api.repository.report.jpa;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaReportRepository extends JpaRepository<Report, Long> {

    boolean existsByTargetIdAndTargetTypeAndMemberId(Long targetId, TargetType targetType, Long memberId);
}
