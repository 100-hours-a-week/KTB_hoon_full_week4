package kakao.bootcamp.fullstack.api.repository.member.jpa;

import java.util.Optional;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod","test"})
@RequiredArgsConstructor
public class JpaMemberRepositoryAdapter implements MemberRepository {

    private final JpaMemberRepository jpaMemberRepository;

    @Override
    public void save(Member member) {
        jpaMemberRepository.save(member);
    }

    @Override
    public Optional<Member> findActiveById(Long id) {
        return jpaMemberRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public boolean existsById(Long id) {
        return jpaMemberRepository.existsById(id);
    }

    @Override
    public Optional<Member> findActiveByEmail(String email) {
        return jpaMemberRepository.findByEmailAndDeletedFalse(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaMemberRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return jpaMemberRepository.existsByNickname(nickname);
    }
}
