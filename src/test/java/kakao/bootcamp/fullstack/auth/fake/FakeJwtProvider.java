package kakao.bootcamp.fullstack.auth.fake;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.security.jwt.provider.JwtProvider;

public class FakeJwtProvider implements JwtProvider {

    private static final long ACCESS_TOKEN_TTL_MILLIS = 600_000L;

    private final AtomicLong sequence = new AtomicLong(0);
    private final Map<String, Issued> issued = new HashMap<>();

    @Override
    public String createAccessToken(Long memberId, String email, Role role, String familyId) {
        long n = sequence.incrementAndGet();
        String token = "access-token-" + n;
        issued.put(
                token,
                new Issued(
                        memberId,
                        email,
                        role,
                        familyId,
                        "jti-" + n,
                        System.currentTimeMillis() + ACCESS_TOKEN_TTL_MILLIS));
        return token;
    }

    @Override
    public void validateToken(String token) {}

    @Override
    public Long getMemberId(String token) {
        return require(token).memberId();
    }

    @Override
    public String getEmail(String token) {
        return require(token).email();
    }

    @Override
    public String getJti(String token) {
        return require(token).jti();
    }

    @Override
    public String getFid(String token) {
        return require(token).familyId();
    }

    @Override
    public Role getRole(String token) {
        return require(token).role();
    }

    @Override
    public long getExpirationMillis(String token) {
        return require(token).expirationMillis();
    }

    private Issued require(String token) {
        Issued value = issued.get(token);
        if (value == null) {
            throw new IllegalArgumentException("발급하지 않은 토큰: " + token);
        }
        return value;
    }

    private record Issued(
            Long memberId,
            String email,
            Role role,
            String familyId,
            String jti,
            long expirationMillis) {}
}
