package kakao.bootcamp.fullstack.auth.fake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.global.security.jwt.TokenBlacklist;

public class FakeTokenBlacklist implements TokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, long tokenExpiresAt) {
        blacklist.put(jti, tokenExpiresAt);
    }

    @Override
    public boolean exists(String jti) {
        return blacklist.containsKey(jti);
    }

    @Override
    public int evictExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
        return before - blacklist.size();
    }
}
