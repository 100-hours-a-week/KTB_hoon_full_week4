package kakao.bootcamp.fullstack.api.domain.member;

import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
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
    private Role role = Role.ROLE_USER;

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
            throw new BusinessException(CommonErrorCode.ALREADY_ASSIGNED_ID);
        }
        this.id = id;
    }

    public void updateProfile(String nickname, String profileImgUrl){
        this.profileImgUrl = profileImgUrl;
        this.nickname = nickname;
    }

    public void updatePassword(String encodedPassword){
        this.encodedPassword = encodedPassword;
    }

    // TODO : 도메인에서 필드 유효성 검사 필요
    public static Member create(
            String email,
            String encodedPassword,
            String nickname,
            String profileImageUrl
    ) {
        return new Member(email, encodedPassword, nickname, profileImageUrl);
    }
}
