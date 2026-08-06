package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaSearchRepositoryAdapter implements SearchRepository {

    private final JpaSearchRepository jpaSearchRepository;

    @Override
    public List<Post> searchPostPage(PostSearchCond cond) {
        return jpaSearchRepository.searchActivePostPage(
                cond.keyword(),
                cond.category(),
                cond.meetingType(),
                cond.recruitStatus(),
                cond.sido(),
                cond.sigungu(),
                cond.createdFrom(),
                cond.createdTo(),
                cond.cursor(),
                PageRequest.of(0, cond.size().intValue()));
    }
}
