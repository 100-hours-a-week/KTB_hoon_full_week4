package kakao.bootcamp.fullstack.global.jwt.annotation;

import jakarta.validation.constraints.Pattern;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^\\S{1,10}$")
public @interface ValidNickname {
    String message() default "invalid nickname";
}