package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaSearchRepositoryAdapter implements SearchRepository {

    private final JpaSearchRepository jpaSearchRepository;

    @Override
    public List<Post> searchPostPage(
            String keyword, PostCategory category, Long cursor, Long size) {
        Pageable pageable = PageRequest.of(0, size.intValue());
        if (cursor == null) {
            return jpaSearchRepository.searchActivePostPage(keyword, category, pageable);
        }
        return jpaSearchRepository.searchActivePostPageBeforeCursor(
                keyword, category, cursor, pageable);
    }
}
