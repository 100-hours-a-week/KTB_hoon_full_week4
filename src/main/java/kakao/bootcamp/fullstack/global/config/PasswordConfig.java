package kakao.bootcamp.fullstack.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordHasher passwordHasher() {
        return new SimplePasswordHasher();
    }
}
