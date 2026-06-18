package kakao.bootcamp.fullstack.api.repository.member;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;

public interface MemberRepository {
    void save(Member member);
    Optional<Member> findActiveById(Long id);
    boolean existsById(Long id);
    Optional<Member> findActiveByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
