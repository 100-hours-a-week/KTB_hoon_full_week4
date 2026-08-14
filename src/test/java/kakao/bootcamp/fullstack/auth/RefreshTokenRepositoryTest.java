package kakao.bootcamp.fullstack.auth;

import static kakao.bootcamp.fullstack.auth.fixture.RefreshTokenFixture.active;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;
import kakao.bootcamp.fullstack.api.repository.auth.RefreshTokenRepository;
import kakao.bootcamp.fullstack.api.repository.auth.jpa.JpaRefreshTokenRepositoryAdapter;
import kakao.bootcamp.fullstack.auth.fixture.RefreshTokenFixture;
import kakao.bootcamp.fullstack.global.config.JpaConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaRefreshTokenRepositoryAdapter.class, JpaConfig.class})
public class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @Test
    @DisplayName("RT를 저장하면 id가 할당된다")
    void savesAndAssignsId() {
        // given
        RefreshToken token = active("family-a", "hash-1");

        // when
        refreshTokenRepository.save(token);

        // then
        assertThat(token.isNew()).isFalse();
        assertThat(token.getId()).isNotNull();
    }

    @Test
    @DisplayName("tokenHash로 활성 RT를 조회한다")
    void findsActiveByTokenHash() {
        // given
        refreshTokenRepository.save(active("family-a", "hash-1"));

        // when
        Optional<RefreshToken> result = refreshTokenRepository.findNotDeletedByTokenHash("hash-1");

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getFamilyId()).isEqualTo("family-a");
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("소프트 삭제된 RT는 tokenHash로 조회되지 않는다")
    void doesNotFindDeletedToken() {
        // given
        RefreshToken token = active("family-a", "hash-1");
        token.delete();
        refreshTokenRepository.save(token);

        // when
        Optional<RefreshToken> result = refreshTokenRepository.findNotDeletedByTokenHash("hash-1");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("revokeAllByFamilyId는 해당 family의 모든 RT를 revoke한다")
    void revokesAllByFamilyId() {
        // given
        refreshTokenRepository.save(active("family-a", "hash-a1"));
        refreshTokenRepository.save(active("family-a", "hash-a2"));
        refreshTokenRepository.save(active("family-b", "hash-b1"));

        // when
        refreshTokenRepository.revokeAllByFamilyId("family-a");

        // then
        assertThat(
                        refreshTokenRepository
                                .findNotDeletedByTokenHash("hash-a1")
                                .orElseThrow()
                                .isRevoked())
                .isTrue();
        assertThat(
                        refreshTokenRepository
                                .findNotDeletedByTokenHash("hash-a2")
                                .orElseThrow()
                                .isRevoked())
                .isTrue();
        assertThat(
                        refreshTokenRepository
                                .findNotDeletedByTokenHash("hash-b1")
                                .orElseThrow()
                                .isRevoked())
                .isFalse();
    }

    @Test
    @DisplayName("revokeIfNotRevoked는 처음 한 번만 성공한다")
    void revokesOnlyOnce() {
        // given
        RefreshToken token = active("family-a", "hash-1");
        refreshTokenRepository.save(token);

        // when
        boolean first = refreshTokenRepository.revokeIfNotRevoked(token.getId());
        boolean second = refreshTokenRepository.revokeIfNotRevoked(token.getId());

        // then
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(refreshTokenRepository.findNotDeletedByTokenHash("hash-1").orElseThrow())
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }

    @Test
    @DisplayName("revokeIfNotRevoked는 이미 폐기된 RT에 대해 실패한다")
    void doesNotRevokeAlreadyRevokedToken() {
        // given
        RefreshToken token = active("family-a", "hash-1");
        refreshTokenRepository.save(token);
        refreshTokenRepository.revokeAllByFamilyId("family-a");

        // when
        boolean revoked = refreshTokenRepository.revokeIfNotRevoked(token.getId());

        // then
        assertThat(revoked).isFalse();
    }

    @Test
    @DisplayName("회원의 폐기되지 않은 RT family id를 중복 없이 조회한다")
    void findsNotRevokedFamilyIdsByMemberId() {
        // given
        refreshTokenRepository.save(active("family-a", "hash-a1"));
        refreshTokenRepository.save(active("family-a", "hash-a2"));
        refreshTokenRepository.save(active("family-b", "hash-b1"));
        refreshTokenRepository.save(active("family-c", "hash-c1"));
        refreshTokenRepository.revokeAllByFamilyId("family-c");

        // when
        List<String> familyIds =
                refreshTokenRepository.findNotRevokedFamilyIdsByMemberId(
                        RefreshTokenFixture.MEMBER_ID);

        // then
        assertThat(familyIds).containsExactlyInAnyOrder("family-a", "family-b");
    }

    @Test
    @DisplayName("다른 회원의 family id는 조회되지 않는다")
    void doesNotFindFamilyIdsOfOtherMember() {
        // given
        refreshTokenRepository.save(active("family-a", "hash-a1"));

        // when
        List<String> familyIds = refreshTokenRepository.findNotRevokedFamilyIdsByMemberId(999L);

        // then
        assertThat(familyIds).isEmpty();
    }

    @Test
    @DisplayName("revoke된 RT도 삭제되지 않았으면 tokenHash로 여전히 조회된다 (revoked ≠ deleted)")
    void revokedTokenIsStillFound() {
        // given
        refreshTokenRepository.save(active("family-a", "hash-1"));

        // when
        refreshTokenRepository.revokeAllByFamilyId("family-a");

        // then
        assertThat(refreshTokenRepository.findNotDeletedByTokenHash("hash-1")).isPresent();
    }
}
