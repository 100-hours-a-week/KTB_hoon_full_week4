package kakao.bootcamp.fullstack.global.security.jwt.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Pattern(regexp = "^\\S{1,10}$", message = ValidationCode.INVALID_NICKNAME_FORMAT)
@Constraint(validatedBy = {})
public @interface ValidNickname {
    String message() default "invalid nickname";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}