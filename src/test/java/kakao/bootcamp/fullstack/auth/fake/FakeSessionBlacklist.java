package kakao.bootcamp.fullstack.auth.fake;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.global.security.jwt.SessionBlacklist;

public class FakeSessionBlacklist implements SessionBlacklist {

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
