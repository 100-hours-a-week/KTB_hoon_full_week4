package kakao.bootcamp.fullstack.global.config;

import kakao.bootcamp.fullstack.global.hasher.PasswordHasher;
import kakao.bootcamp.fullstack.global.hasher.SimplePasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordHasher passwordHasher() {
        return new SimplePasswordHasher();
    }
}
