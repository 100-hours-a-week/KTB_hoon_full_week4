package kakao.bootcamp.fullstack.member;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.member.jpa.JpaMemberRepositoryAdapter;
import kakao.bootcamp.fullstack.global.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaMemberRepositoryAdapter.class, JpaConfig.class})
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.create("test@example.com", "encoded", "nick", "url");
    }

    @Test
    @DisplayName("회원을 저장하면 id가 할당된다")
    void savesMemberAndAssignsId() {
       // when
        memberRepository.save(member);

        // then
        assertThat(member.isNew()).isFalse();
        assertThat(member.getId()).isNotNull();
    }

    @Test
    @DisplayName("저장한 필드 값이 그대로 유지된다")
    void savesMemberWithGivenFields() {
        // when
        memberRepository.save(member);
        Member found = memberRepository.findActiveById(member.getId()).orElseThrow();

        // then
        assertThat(found.getEmail()).isEqualTo("test@example.com");
        assertThat(found.getEncodedPassword()).isEqualTo("encoded");
        assertThat(found.getNickname()).isEqualTo("nick");
        assertThat(found.getProfileImgUrl()).isEqualTo("url");
    }

    @Test
    @DisplayName("이메일로 활성 회원을 조회한다")
    void findsActiveByEmail() {
        // given
        memberRepository.save(member);

        // when
        Optional<Member> found = memberRepository.findActiveByEmail("test@example.com");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(member.getId());
    }

    @Test
    @DisplayName("삭제된 회원은 조회되지 않는다")
    void doesNotFindDeletedMember() {
        // given
        member.delete();
        memberRepository.save(member);

        // when
        Optional<Member> found = memberRepository.findActiveByEmail("test@example.com");

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("id로 해당 회원이 존재함을 확인한다")
    void existsActiveById() {
        // given
        memberRepository.save(member);

        //when
        boolean found = memberRepository.existsById(member.getId());

        //then
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("삭제된 회원이어도 id로 존재를 확인하면 true를 반환한다")
    void existsByIdEvenWhenDeleted() {
        // given
        member.delete();
        memberRepository.save(member);

        // when
        boolean found = memberRepository.existsById(member.getId());

        // then
        assertThat(found).isTrue();
    }


    @Test
    @DisplayName("이메일로 해당 회원이 존재함을 확인한다")
    void existsByEmail() {
        // given
        memberRepository.save(member);

        //when
        boolean found = memberRepository.existsByEmail(member.getEmail());

        //then
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("닉네임으로 해당 회원이 존재함을 확인한다")
    void existsByNickname() {
        // given
        memberRepository.save(member);

        //when
        boolean found = memberRepository.existsByNickname(member.getNickname());

        //then
        assertThat(found).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 id면 false를 반환한다")
    void doesNotExistByIdWhenNotSaved() {
        // given
        Long notSavedId = 999L;

        // when
        boolean found = memberRepository.existsById(notSavedId);

        // then
        assertThat(found).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 이메일이면 false를 반환한다")
    void doesNotExistByEmailWhenNotSaved() {
        // given
        String notSavedEmail = "notfound@example.com";

        // when
        boolean found = memberRepository.existsByEmail(notSavedEmail);

        // then
        assertThat(found).isFalse();
    }


    @Test
    @DisplayName("삭제된 회원이어도 이메일로 존재를 확인하면 true를 반환한다")
    void existsByEmailEvenWhenDeleted() {
        // given
        member.delete();
        memberRepository.save(member);

        // when
        boolean found = memberRepository.existsByEmail(member.getEmail());

        // then
        assertThat(found).isTrue();
    }
}
