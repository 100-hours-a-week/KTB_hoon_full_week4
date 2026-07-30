package kakao.bootcamp.fullstack.auth.fixture;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.auth.RefreshToken;

public class RefreshTokenFixture {

    public static final Long MEMBER_ID = 1L;

    public static RefreshToken active(String familyId, String tokenHash) {
        return RefreshToken.create(MEMBER_ID, familyId, tokenHash, LocalDateTime.now().plusDays(1));
    }

    public static RefreshToken expired(String familyId, String tokenHash) {
        return RefreshToken.create(
                MEMBER_ID, familyId, tokenHash, LocalDateTime.now().minusSeconds(1));
    }
}
