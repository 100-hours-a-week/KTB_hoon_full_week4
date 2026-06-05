package kakao.bootcamp.fullstack.global.jwt;

public interface TokenBlacklist {
    void add(String jti, long tokenExpiresAt);
    boolean contains(String jti);
    void removeExpired();
}
