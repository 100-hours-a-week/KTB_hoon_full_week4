package kakao.bootcamp.fullstack.api.repository.auth.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod","test"})
@RequiredArgsConstructor
public class JpaRefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final JpaRefreshTokenRepository jpaRefreshTokenRepository;

    @Override
    public void save(RefreshToken refreshToken) {
        jpaRefreshTokenRepository.save(refreshToken);
    }

    @Override
    public Optional<RefreshToken> findActiveByTokenHash(String tokenHash) {
        return jpaRefreshTokenRepository.findByTokenHashAndDeletedFalse(tokenHash);
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        jpaRefreshTokenRepository.revokeAllByFamilyId(familyId);
    }
}
