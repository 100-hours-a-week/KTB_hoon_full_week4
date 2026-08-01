package kakao.bootcamp.fullstack.api.repository.auth.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndDeletedFalse(String tokenHash);

    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE RefreshToken r SET r.revoked = true "
                    + "WHERE r.familyId = :familyId AND r.revoked = false AND r.deleted = false")
    void revokeAllByFamilyId(@Param("familyId") String familyId);

    @Modifying(clearAutomatically = true)
    @Query(
            "UPDATE RefreshToken r SET r.revoked = true "
                    + "WHERE r.memberId = :memberId AND r.revoked = false AND r.deleted = false")
    void revokeAllByMemberId(@Param("memberId") Long memberId);
}
