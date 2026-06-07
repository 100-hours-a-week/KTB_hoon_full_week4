package kakao.bootcamp.fullstack.api.domain.member;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {
    ROLE_USER("0", "role_user"),
    ROLE_ADMIN("1", "role_admin");
    ;

    private final String code;
    private final String label;

    public static Optional<Role> from(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst();
    }
}