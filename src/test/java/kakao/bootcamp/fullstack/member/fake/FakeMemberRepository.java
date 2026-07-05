package kakao.bootcamp.fullstack.member.fake;

import java.util.Optional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;

public class FakeMemberRepository implements MemberRepository {

    private final Map<Long, Member> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    @Override
    public void save(Member member) {
        if (member.isNew()) {
            member.assignId(sequence.incrementAndGet());
        }
        store.put(member.getId(), member);
    }

    @Override
    public Optional<Member> findActiveById(Long id) {
        return findActive(id).filter(member -> !member.isDeleted());
    }

    @Override
    public boolean existsById(Long id) {
        return store.containsKey(id);
    }

    @Override
    public Optional<Member> findActiveByEmail(String email) {
        return store.values().stream()
                .filter(member -> !member.isDeleted())
                .filter(member -> member.getEmail().equals(email))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.values().stream()
                .filter(member -> !member.isDeleted())
                .anyMatch(member -> member.getEmail().equals(email));
    }

    @Override
    public boolean existsByNickname(String nickname) {
        return store.values().stream()
                .filter(member -> !member.isDeleted())
                .anyMatch(member -> member.getNickname().equals(nickname));
    }

    private Optional<Member> findActive(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }
}