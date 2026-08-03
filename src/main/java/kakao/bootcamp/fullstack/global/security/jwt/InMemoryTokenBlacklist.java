package kakao.bootcamp.fullstack.global.security.jwt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "prod"})
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, long tokenExpiresAt) {
        blacklist.put(jti, tokenExpiresAt);
    }

    @Override
    public boolean exists(String jti) {
        Long expiresAt = blacklist.get(jti);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            blacklist.remove(jti, expiresAt);
            return false;
        }
        return true;
    }

    @Override
    public int evictExpired() {
        long now = System.currentTimeMillis();
        int before = blacklist.size();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
        return before - blacklist.size();
    }
}
