package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
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
    public List<Post> searchPostPage(PostSearchCond cond) {
        Pageable pageable = PageRequest.of(0, cond.size().intValue());
        if (cond.keyword() == null) {
            return jpaSearchRepository.findActivePostPage(
                    cond.category(),
                    cond.meetingType(),
                    cond.recruitStatus(),
                    cond.sido(),
                    cond.sigungu(),
                    cond.createdFrom(),
                    cond.createdTo(),
                    cond.cursorCreatedAt(),
                    cond.cursorId(),
                    pageable);
        }
        return jpaSearchRepository.searchActivePostPage(
                sanitizeKeyword(cond.keyword()),
                cond.category() == null ? null : cond.category().name(),
                cond.meetingType() == null ? null : cond.meetingType().name(),
                cond.recruitStatus() == null ? null : cond.recruitStatus().name(),
                cond.sido(),
                cond.sigungu(),
                cond.createdFrom(),
                cond.createdTo(),
                cond.cursorCreatedAt(),
                cond.cursorId(),
                pageable);
    }

    // FULLTEXT BOOLEAN MODE 의 phrase 구문(따옴표)이 깨지지 않도록 제거한다.
    private String sanitizeKeyword(String keyword) {
        return keyword.replace("\"", "");
    }
}
