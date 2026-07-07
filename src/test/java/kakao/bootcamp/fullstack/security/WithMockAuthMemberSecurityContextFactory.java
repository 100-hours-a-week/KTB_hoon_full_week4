package kakao.bootcamp.fullstack.security;

import java.util.List;
import kakao.bootcamp.fullstack.global.security.dto.AuthMember;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockAuthMemberSecurityContextFactory implements
        WithSecurityContextFactory<WithMockAuthMember> {

    @Override
    public SecurityContext createSecurityContext(WithMockAuthMember annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        AuthMember principal = new AuthMember(annotation.memberId(), annotation.email(), annotation.role());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, "", List.of(new SimpleGrantedAuthority(annotation.role()))
        );
        context.setAuthentication(auth);
        return context;
    }
}