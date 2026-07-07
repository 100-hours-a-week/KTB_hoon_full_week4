package kakao.bootcamp.fullstack.security;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAuthMemberSecurityContextFactory.class)
public @interface WithMockAuthMember {
    long memberId() default 1L;
    String email() default "test@test.com";
    String role() default "ROLE_USER";
}