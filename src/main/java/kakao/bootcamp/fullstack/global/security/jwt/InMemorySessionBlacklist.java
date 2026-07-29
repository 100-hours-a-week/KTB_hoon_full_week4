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
        return blacklist.containsKey(familyId);
    }
}
