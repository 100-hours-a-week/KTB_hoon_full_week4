package kakao.bootcamp.fullstack.member.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;

public class MemberFixture {

    public static Member activeMember(Long id) {
        Member member = Member.create("test@example.com", "encoded", "nick", "url");
        member.assignId(id);
        return member;
    }
}
