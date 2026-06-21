package kakao.bootcamp.fullstack.api.repository.report.jpa;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.report.Report;
import kakao.bootcamp.fullstack.api.repository.report.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("prod")
@RequiredArgsConstructor
public class JpaReportRepositoryAdapter implements ReportRepository {

    private final JpaReportRepository jpaReportRepository;

    @Override
    public void save(Report report) {
        jpaReportRepository.save(report);
    }

    @Override
    public boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId) {
        return jpaReportRepository.existsByTargetAndMember(targetId, targetType, memberId);
    }
}
