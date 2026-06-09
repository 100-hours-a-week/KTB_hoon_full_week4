package kakao.bootcamp.fullstack.global.hasher;

public interface PasswordEncoder {
    String hash(String rawPassword);
    boolean matches(String rawPassword, String hashedPassword);
}
