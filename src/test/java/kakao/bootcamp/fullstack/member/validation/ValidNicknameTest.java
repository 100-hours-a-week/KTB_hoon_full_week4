package kakao.bootcamp.fullstack.member.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;
import kakao.bootcamp.fullstack.global.security.jwt.annotation.ValidNickname;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ValidNicknameTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    record TestTarget(@ValidNickname String nickname) {}

    private Set<ConstraintViolation<TestTarget>> validate(String nickname) {
        return validator.validate(new TestTarget(nickname));
    }

    @Test
    @DisplayName("공백 없이 10자 이내면 통과한다")
    void passesWhenValidNickname() {
        Set<ConstraintViolation<TestTarget>> violations = validate("홍길동");

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("길이가 1자면 통과한다")
    void passesWhenLengthIsOne() {
        String nickname = "가";
        assertThat(nickname).hasSize(1);

        Set<ConstraintViolation<TestTarget>> violations = validate(nickname);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("길이가 10자면 통과한다")
    void passesWhenLengthIsTen() {
        String nickname = "가나다라마바사아자차";
        assertThat(nickname).hasSize(10);

        Set<ConstraintViolation<TestTarget>> violations = validate(nickname);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("길이가 11자면 실패한다")
    void failsWhenLengthIsEleven() {
        String nickname = "가나다라마바사아자차카";
        assertThat(nickname).hasSize(11);

        Set<ConstraintViolation<TestTarget>> violations = validate(nickname);

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_NICKNAME_FORMAT);
    }

    @Test
    @DisplayName("빈 문자열이면 실패한다")
    void failsWhenEmpty() {
        Set<ConstraintViolation<TestTarget>> violations = validate("");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_NICKNAME_FORMAT);
    }

    @Test
    @DisplayName("공백이 포함되면 실패한다")
    void failsWhenContainsWhitespace() {
        Set<ConstraintViolation<TestTarget>> violations = validate("홍 길동");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_NICKNAME_FORMAT);
    }

    @Test
    @DisplayName("공백만으로 이루어지면 실패한다")
    void failsWhenOnlyWhitespace() {
        Set<ConstraintViolation<TestTarget>> violations = validate("   ");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_NICKNAME_FORMAT);
    }

    @Test
    @DisplayName("탭이나 개행 문자가 포함되면 실패한다")
    void failsWhenContainsTabOrNewline() {
        Set<ConstraintViolation<TestTarget>> violations = validate("홍길\t동");

        assertThat(violations).hasSize(1);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly(ValidationCode.INVALID_NICKNAME_FORMAT);
    }

    @Test
    @DisplayName("null이면 통과한다")
    void passesWhenNull() {
        // Bean Validation 스펙 권장: null은 @Pattern이 관여 안 함 (별도 @NotBlank가 담당)
        Set<ConstraintViolation<TestTarget>> violations = validate(null);

        assertThat(violations).isEmpty();
    }
}