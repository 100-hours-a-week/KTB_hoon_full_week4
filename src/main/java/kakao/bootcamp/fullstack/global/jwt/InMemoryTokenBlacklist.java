package kakao.bootcamp.fullstack.global.jwt;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("local")
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, long tokenExpiresAt) {
        blacklist.put(jti, tokenExpiresAt);
    }

    @Override
    public boolean contains(String jti) {
        removeExpired();
        Long expiration = blacklist.get(jti);
        return expiration != null;
    }

    @Override
    public void removeExpired() {
        long now = System.currentTimeMillis() / 1000;
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}