package kakao.bootcamp.fullstack.global.security.hasher;

public interface PasswordHasher {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
