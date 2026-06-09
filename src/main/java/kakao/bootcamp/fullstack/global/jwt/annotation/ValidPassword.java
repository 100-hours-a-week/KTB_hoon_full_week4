package kakao.bootcamp.fullstack.global.jwt.annotation;

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
@Constraint(validatedBy = {})
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,20}$", message = ValidationCode.INVALID_PASSWORD_FORMAT)
public @interface ValidPassword {
    String message() default "invalid password format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
