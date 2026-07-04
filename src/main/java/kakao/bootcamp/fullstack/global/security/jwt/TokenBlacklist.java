package kakao.bootcamp.fullstack.global.security.jwt;

public interface TokenBlacklist {
    void add(String jti, long tokenExpiresAt);
    boolean exists(String jti);
}
