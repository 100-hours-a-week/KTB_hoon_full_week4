package kakao.bootcamp.fullstack.api.repository.member;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.global.generator.AtomicLongIdGenerator;
import kakao.bootcamp.fullstack.global.generator.IdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("local")
public class InMemoryMemberRepository implements MemberRepository {

    private final IdGenerator idGenerator = new AtomicLongIdGenerator();
    private final Map<Long, Member> members = new ConcurrentHashMap<>();

    @Override
    public void save(Member member) {
        if (member.isNew()) {
            Long id = idGenerator.nextId();
            member.assignId(id);
        }
        members.put(member.getId(), member);
    }

    @Override
    public Optional<Member> findActiveById(Long id) {
        return members.values().stream()
                .filter(member -> !member.isDeleted())
                .filter(m -> m.getId().equals(id))
                .findFirst();
    }

    @Override
    public boolean existsById(Long id) {
        return members.containsKey(id);
    }

    @Override
    public Optional<Member> findActiveByEmail(String email) {
        return members.values()
                .stream()
                .filter(member -> member.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return members.entrySet()
                .stream()
                .anyMatch(entry -> entry.getValue().getEmail().equals(email));
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return members.entrySet()
                .stream()
                .anyMatch(entry -> entry.getValue().getNickname().equals(nickname));
    }
}
