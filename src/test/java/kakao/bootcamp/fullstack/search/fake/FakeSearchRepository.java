package kakao.bootcamp.fullstack.search.fake;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;

public class FakeSearchRepository implements SearchRepository {

    private final Map<Long, Post> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    public void save(Post post) {
        if (post.isNew()) {
            post.assignId(sequence.incrementAndGet());
        }
        store.put(post.getId(), post);
    }

    public void clear() {
        store.clear();
        sequence.set(0);
    }

    @Override
    public List<Post> searchPostPage(PostSearchCond cond) {
        return store.values().stream()
                .filter(post -> !post.isDeleted())
                .filter(post -> !post.isBlinded())
                .filter(matches(cond))
                .sorted(
                        Comparator.comparing(Post::getCreatedAt)
                                .thenComparing(Post::getId)
                                .reversed())
                .limit(cond.size())
                .toList();
    }

    private Predicate<Post> matches(PostSearchCond cond) {
        String lowered = cond.keyword() == null ? null : cond.keyword().toLowerCase();
        return post ->
                (lowered == null
                                || post.getTitle().toLowerCase().contains(lowered)
                                || post.getContent().toLowerCase().contains(lowered))
                        && (cond.category() == null || post.getCategory() == cond.category())
                        && (cond.meetingType() == null
                                || post.getMeetingType() == cond.meetingType())
                        && (cond.recruitStatus() == null
                                || post.getRecruitStatus() == cond.recruitStatus())
                        && (cond.sido() == null || cond.sido().equals(sido(post)))
                        && (cond.sigungu() == null || cond.sigungu().equals(sigungu(post)))
                        && (cond.createdFrom() == null
                                || !post.getCreatedAt().isBefore(cond.createdFrom()))
                        && (cond.createdTo() == null
                                || post.getCreatedAt().isBefore(cond.createdTo()))
                        && afterCursor(cond, post);
    }

    private boolean afterCursor(PostSearchCond cond, Post post) {
        if (cond.cursorCreatedAt() == null) {
            return true;
        }
        if (post.getCreatedAt().isBefore(cond.cursorCreatedAt())) {
            return true;
        }
        return post.getCreatedAt().equals(cond.cursorCreatedAt()) && post.getId() < cond.cursorId();
    }

    private String sido(Post post) {
        Address address = post.getAddress();
        return address == null ? null : address.getSido();
    }

    private String sigungu(Post post) {
        Address address = post.getAddress();
        return address == null ? null : address.getSigungu();
    }
}
