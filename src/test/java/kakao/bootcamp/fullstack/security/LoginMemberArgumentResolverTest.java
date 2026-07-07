package kakao.bootcamp.fullstack.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import kakao.bootcamp.fullstack.global.resolver.LoginMemberArgumentResolver;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class LoginMemberArgumentResolverTest {

    private final LoginMemberArgumentResolver resolver = new LoginMemberArgumentResolver();

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("SecurityContext에서_AuthMember를_꺼낸다")
    void getAuthMemberFromSecurityContext() throws Exception {
        AuthMember authMember = new AuthMember(1L, "test@test.com","ROLE_USER");
        Authentication auth = new UsernamePasswordAuthenticationToken(authMember, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        AuthMember result = resolver.resolveArgument(null, null, null, null);
        assertThat(result).isEqualTo(authMember);
    }
}
