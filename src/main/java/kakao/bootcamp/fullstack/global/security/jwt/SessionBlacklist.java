package kakao.bootcamp.fullstack.global.security.jwt;

public interface SessionBlacklist {
    void add(String familyId, long tokenExpiresAt);

    boolean exists(String familyId);
}
