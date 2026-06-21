package kakao.bootcamp.fullstack.api.repository.report.jpa;

import kakao.bootcamp.fullstack.api.domain.common.TargetType;
import kakao.bootcamp.fullstack.api.domain.report.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Report r "
            + "WHERE r.targetId = :targetId AND r.targetType = :targetType AND r.memberId = :memberId")
    boolean existsByTargetAndMember(
            @Param("targetId") Long targetId,
            @Param("targetType") TargetType targetType,
            @Param("memberId") Long memberId
    );
}
