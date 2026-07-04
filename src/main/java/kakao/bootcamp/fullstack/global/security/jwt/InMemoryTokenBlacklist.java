package kakao.bootcamp.fullstack.global.security.jwt;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile({"local","prod"})
public class InMemoryTokenBlacklist implements TokenBlacklist {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String jti, long tokenExpiresAt) {
        blacklist.put(jti, tokenExpiresAt);
    }

    @Override
    public boolean exists(String jti) {
        return blacklist.containsKey(jti);
    }
}