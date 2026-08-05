package kakao.bootcamp.fullstack.report.fake;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.report.Report;
import kakao.bootcamp.fullstack.api.repository.report.ReportRepository;

public class FakeReportRepository implements ReportRepository {

    private final Map<Long, Report> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Report report) {
        if (report.isNew()) {
            report.assignId(sequence.incrementAndGet());
        }
        store.put(report.getId(), report);
    }

    @Override
    public boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId) {
        return store.values().stream()
                .anyMatch(
                        report ->
                                Objects.equals(report.getTargetId(), targetId)
                                        && report.getTargetType() == targetType
                                        && Objects.equals(report.getMemberId(), memberId));
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}
