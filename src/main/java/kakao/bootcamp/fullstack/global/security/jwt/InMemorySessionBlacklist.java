package kakao.bootcamp.fullstack.global.security.jwt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "prod"})
public class InMemorySessionBlacklist implements SessionBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String familyId, long tokenExpiresAt) {
        blacklist.put(familyId, tokenExpiresAt);
    }

    @Override
    public boolean exists(String familyId) {
        Long expiresAt = blacklist.get(familyId);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            blacklist.remove(familyId, expiresAt);
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
