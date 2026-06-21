package kakao.bootcamp.fullstack.api.repository.member;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndDeletedFalse(Long id);

    Optional<Member> findByEmailAndDeletedFalse(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
