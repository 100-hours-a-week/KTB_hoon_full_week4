package kakao.bootcamp.fullstack.global.security.hasher;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
public class SpringSecurityPasswordEncoderAdapter implements PasswordEncoder {

    private final PasswordHasher passwordHasher;

    @Override
    public String encode(CharSequence rawPassword) {
        return passwordHasher.hash(rawPassword.toString());
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return passwordHasher.matches(rawPassword.toString(), encodedPassword);
    }
}
