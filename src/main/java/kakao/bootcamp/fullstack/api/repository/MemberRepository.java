package kakao.bootcamp.fullstack.api.repository;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;

public interface MemberRepository {
    void save(Member member);
    Optional<Member> findById(Long id);
    boolean existsById(Long id);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}
