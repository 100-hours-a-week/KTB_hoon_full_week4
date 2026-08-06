package kakao.bootcamp.fullstack.api.repository.search.inmemory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.post.inmemory.InMemoryPostRepository;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("inmemory")
@RequiredArgsConstructor
public class InMemorySearchRepository implements SearchRepository {

    private final InMemoryPostRepository inMemoryPostRepository;

    @Override
    public List<Post> searchPostPage(PostSearchCond cond) {
        return inMemoryPostRepository.findAllActive().stream()
                .filter(post -> !post.isBlinded())
                .filter(matches(cond))
                .sorted(Comparator.comparingLong(Post::getId).reversed())
                .limit(cond.size())
                .toList();
    }

    private Predicate<Post> matches(PostSearchCond cond) {
        String lowered = cond.keyword().toLowerCase();
        return post ->
                (post.getTitle().toLowerCase().contains(lowered)
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
                        && (cond.cursor() == null || post.getId() < cond.cursor());
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
