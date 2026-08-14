package kakao.bootcamp.fullstack.api.repository.auth.jpa;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;

    @Override
    public void save(RefreshToken refreshToken) {
        jpaRefreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findNotDeletedByTokenHash(String tokenHash) {
        return jpaRefreshTokenRepository.findByTokenHashAndDeletedFalse(tokenHash);
    }

    @Override
    public List<String> findNotRevokedFamilyIdsByMemberId(Long memberId) {
        return jpaRefreshTokenRepository.findNotRevokedFamilyIdsByMemberId(memberId);
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        jpaRefreshTokenRepository.revokeAllByFamilyId(familyId);
    }

    @Override
    public void revokeAllByMemberId(Long memberId) {
        jpaRefreshTokenRepository.revokeAllByMemberId(memberId);
    }
}
