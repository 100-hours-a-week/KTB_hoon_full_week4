package kakao.bootcamp.fullstack.auth.fake;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;

public class FakeRefreshTokenRepository implements RefreshTokenRepository {

    private final Map<Long, RefreshToken> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(RefreshToken refreshToken) {
        if (refreshToken.isNew()) {
            refreshToken.assignId(sequence.incrementAndGet());
        }
        store.put(refreshToken.getId(), refreshToken);
    }

    @Override
    public Optional<RefreshToken> findActiveByTokenHash(String tokenHash) {
        return store.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getTokenHash(), tokenHash))
                .findFirst();
    }

    @Override
    public void revokeAllByFamilyId(String familyId) {
        store.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getFamilyId(), familyId))
                .forEach(RefreshToken::revoke);
    }

    @Override
    public void revokeAllByMemberId(Long memberId) {
        store.values().stream()
                .filter(token -> !token.isDeleted())
                .filter(token -> Objects.equals(token.getMemberId(), memberId))
                .forEach(RefreshToken::revoke);
    }
}
