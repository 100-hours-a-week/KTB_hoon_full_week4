package kakao.bootcamp.fullstack.api.repository;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.post.Report;
import kakao.bootcamp.fullstack.api.domain.post.TargetType;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryReportRepository implements ReportRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, Report> reports = new ConcurrentHashMap<>();

    @Override
    public void save(Report report) {
        if (report.isNew()) {
            Long id = idGenerator.nextId();
            report.assignId(id);
        }
        reports.put(report.getId(), report);
    }

    @Override
    public boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId) {
        return reports.values()
                .stream()
                .anyMatch(report ->
                        Objects.equals(report.getTargetId(), targetId)
                                && report.getTargetType() == targetType
                                && Objects.equals(report.getMemberId(), memberId)
                );
    }
}
