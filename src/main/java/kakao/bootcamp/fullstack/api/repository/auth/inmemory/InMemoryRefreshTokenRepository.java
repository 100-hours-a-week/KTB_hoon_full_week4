package kakao.bootcamp.fullstack.api.repository.auth.inmemory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryRefreshTokenRepository implements RefreshTokenRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, RefreshToken> refreshTokens = new ConcurrentHashMap<>();

    @Override
    public void save(RefreshToken refreshToken) {
        if (refreshToken.isNew()) {
            Long id = idGenerator.nextId();
            refreshToken.assignId(id);
        }
        refreshTokens.put(refreshToken.getId(), refreshToken);
    }

    @Override
    public Optional<RefreshToken> findActiveByTokenHash(String tokenHash) {
        return refreshTokens.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getTokenHash(), tokenHash))
                .findFirst();
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        refreshTokens.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getFamilyId(), familyId))
                .forEach(RefreshToken::revoke);
    }

    @Override
    public void revokeAllByMemberId(Long memberId) {
        refreshTokens.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getMemberId(), memberId))
                .forEach(RefreshToken::revoke);
    }
}
