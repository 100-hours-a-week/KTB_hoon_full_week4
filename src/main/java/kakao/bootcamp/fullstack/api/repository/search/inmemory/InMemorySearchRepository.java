package kakao.bootcamp.fullstack.api.repository.search.inmemory;

import java.util.Comparator;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.repository.post.inmemory.InMemoryPostRepository;
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
    public List<Post> searchPostPage(
            String keyword, PostCategory category, Long cursor, Long size) {
        String lowered = keyword.toLowerCase();
        return inMemoryPostRepository.findAllActive().stream()
                .filter(post -> !post.isBlinded())
                .filter(post -> category == null || post.getCategory() == category)
                .filter(post -> cursor == null || post.getId() < cursor)
                .filter(
                        post ->
                                post.getTitle().toLowerCase().contains(lowered)
                                        || post.getContent().toLowerCase().contains(lowered))
                .sorted(Comparator.comparingLong(Post::getId).reversed())
                .limit(size)
                .toList();
    }
}
