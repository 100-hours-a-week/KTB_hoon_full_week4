package kakao.bootcamp.fullstack.member.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;

public class MemberFixture {

    public static Member activeMember() {
        return Member.create("test@example.com", "encoded", "nick", "url");
    }

    public static Member activeMember(Long id) {
        Member member = activeMember();
        member.assignId(id);
        return member;
    }

    public static Member activeMember(String email, String encodedPassword) {
        return Member.create(email, encodedPassword, "nick-" + email, "url");
    }
}
