package kakao.bootcamp.fullstack.api.domain.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false)
    private String profileImgUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
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
