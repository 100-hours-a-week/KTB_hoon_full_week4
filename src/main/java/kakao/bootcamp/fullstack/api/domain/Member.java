package kakao.bootcamp.fullstack.api.domain;

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
    private String password;
    private String profileImgUrl;

    public Member(String nickname, String email, String password, String profileImgUrl) {
        this.nickname = nickname;
        this.email = email;
        this.password = password;
        this.profileImgUrl = profileImgUrl;
    }


}
