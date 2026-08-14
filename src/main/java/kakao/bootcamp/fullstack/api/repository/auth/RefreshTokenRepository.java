package kakao.bootcamp.fullstack.api.repository.auth;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;

public interface RefreshTokenRepository {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findNotDeletedByTokenHash(String tokenHash);

    List<String> findNotRevokedFamilyIdsByMemberId(Long memberId);

    void revokeAllByFamilyId(String familyId);

    void revokeAllByMemberId(Long memberId);
}
