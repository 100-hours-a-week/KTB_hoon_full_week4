package kakao.bootcamp.fullstack.member.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.ValidPassword;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ValidPasswordTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    record TestTarget(@ValidPassword String password) {}

    private Set<ConstraintViolation<TestTarget>> validate(String password) {
        return validator.validate(new TestTarget(password));
    }

    @Test
    @DisplayName("모든 조건을 만족하면 통과한다")
    void passesWhenAllConditionsAreMet() {
        Set<ConstraintViolation<TestTarget>> violations = validate("Abcd123!");

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("영문이 없으면 실패한다")
    void failsWhenNoAlphabetCharacter() {
        Set<ConstraintViolation<TestTarget>> violations = validate("12345678!");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("숫자가 없으면 실패한다")
    void failsWhenNoDigit() {
        Set<ConstraintViolation<TestTarget>> violations = validate("Abcdefgh!");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("특수문자가 없으면 실패한다")
    void failsWhenNoSpecialCharacter() {
        Set<ConstraintViolation<TestTarget>> violations = validate("Abcd1234");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("길이가 7자면 실패한다")
    void failsWhenLengthIsSeven() {
        String password = "Ab1!567";
        assertThat(password).hasSize(7);

        Set<ConstraintViolation<TestTarget>> violations = validate(password);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("길이가 8자면 통과한다")
    void passesWhenLengthIsEight() {
        String password = "Ab1!5678";
        assertThat(password).hasSize(8);

        Set<ConstraintViolation<TestTarget>> violations = validate(password);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("길이가 20자면 통과한다")
    void passesWhenLengthIsTwenty() {
        String password = "Ab1!5678901234567890";
        assertThat(password).hasSize(20);

        Set<ConstraintViolation<TestTarget>> violations = validate(password);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("길이가 21자면 실패한다")
    void failsWhenLengthIsTwentyOne() {
        String password = "Ab1!56789012345678900";
        assertThat(password).hasSize(21);

        Set<ConstraintViolation<TestTarget>> violations = validate(password);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    @DisplayName("null이면 통과한다")
    void passesWhenNull() {
        // Bean Validation 스펙 권장: null은 @Pattern이 관여 안 함 (별도 @NotBlank가 담당)
        Set<ConstraintViolation<TestTarget>> violations = validate(null);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("빈 문자열이면 실패한다")
    void failsWhenEmpty() {
        Set<ConstraintViolation<TestTarget>> violations = validate("");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_PASSWORD_FORMAT);
    }
}
