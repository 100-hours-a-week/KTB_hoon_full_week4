package kakao.bootcamp.fullstack.api.domain.member;

import kakao.bootcamp.fullstack.global.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    private Long id;
    private String nickname;
    private String email;
    private String encodedPassword;
    private String profileImgUrl;

    private Member(String email, String encodedPassword, String nickname, String profileImgUrl) {
        this.email = email;
        this.nickname = nickname;
        this.encodedPassword = encodedPassword;
        this.profileImgUrl = profileImgUrl;
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new IllegalStateException("이미 ID가 할당된 회원입니다.");
        }
        this.id = id;
    }

    public static Member create(
            String email,
            String encodedPassword,
            String nickname,
            String profileImageUrl
    ) {
        return new Member(email, encodedPassword, nickname, profileImageUrl);
    }
}
