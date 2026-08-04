package kakao.bootcamp.fullstack.member.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;

public class MemberFixture {

    public static final String EMAIL = "test@example.com";
    public static final String ENCODED_PASSWORD = "encoded";
    public static final String NICKNAME = "nick";
    public static final String PROFILE_IMG_URL = "url";

    public static Member activeMember() {
        return Member.create(EMAIL, ENCODED_PASSWORD, NICKNAME, PROFILE_IMG_URL);
    }

    public static Member activeMember(Long id) {
        Member member = activeMember();
        member.assignId(id);
        return member;
    }

    public static Member activeMember(String email, String encodedPassword) {
        return Member.create(email, encodedPassword, "nick-" + email, PROFILE_IMG_URL);
    }

    public static Member withEncodedPassword(Long id, String encodedPassword) {
        Member member = Member.create(EMAIL, encodedPassword, NICKNAME, PROFILE_IMG_URL);
        member.assignId(id);
        return member;
    }

    public static Member withEmailAndNickname(Long id, String email, String nickname) {
        Member member = Member.create(email, ENCODED_PASSWORD, nickname, PROFILE_IMG_URL);
        member.assignId(id);
        return member;
    }
}
