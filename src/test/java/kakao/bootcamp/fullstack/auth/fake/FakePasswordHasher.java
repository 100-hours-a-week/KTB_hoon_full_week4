package kakao.bootcamp.fullstack.auth.fake;

import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        return rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return rawPassword.equals(hashedPassword);
    }
}
