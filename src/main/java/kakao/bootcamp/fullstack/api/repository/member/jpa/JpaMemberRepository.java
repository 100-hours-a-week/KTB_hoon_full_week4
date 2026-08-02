package kakao.bootcamp.fullstack.api.repository.member.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByIdAndDeletedFalse(Long id);

    Optional<Member> findByEmailAndDeletedFalse(String email);

    // 파생 쿼리라 deleted 필터가 없어 소프트 삭제된 회원도 포함한다(탈퇴 이메일·닉네임 재사용 차단 정책).
    // 포트에서는 existsByEmailIncludingDeleted로 노출한다.
    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
