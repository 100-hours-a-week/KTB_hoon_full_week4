package kakao.bootcamp.fullstack.api.repository;

import kakao.bootcamp.fullstack.api.domain.post.Report;
import kakao.bootcamp.fullstack.api.domain.post.TargetType;

public interface ReportRepository {

    void save(Report report);

    boolean existsByTargetAndMember(Long targetId, TargetType targetType, Long memberId);
}
