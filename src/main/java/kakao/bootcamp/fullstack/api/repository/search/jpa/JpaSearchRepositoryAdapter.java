package kakao.bootcamp.fullstack.api.repository.search.jpa;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;
import kakao.bootcamp.fullstack.api.repository.search.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@Profile({"prod", "test", "local"})
@RequiredArgsConstructor
public class JpaSearchRepositoryAdapter implements SearchRepository {

    private final JpaSearchRepository jpaSearchRepository;
    private final PostSearchIndex postSearchIndex;

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
        if (postSearchIndex.isEnabled()) {
            try {
                return searchViaIndex(cond);
            } catch (RuntimeException e) {
                log.warn("OpenSearch 검색 실패 — FULLTEXT 로 폴백한다", e);
            }
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

    // 색인은 id 만 돌려주고 본문·작성자는 MySQL 에서 채운다. IN 조회는 순서를 보존하지
    // 않으므로 색인이 준 순서(최신순)로 재정렬한다.
    private List<Post> searchViaIndex(PostSearchCond cond) {
        List<Long> ids = postSearchIndex.searchIds(cond);
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> postById =
                jpaSearchRepository.findActiveWithMemberByIdIn(ids).stream()
                        .collect(Collectors.toMap(Post::getId, Function.identity()));
        return ids.stream().map(postById::get).filter(post -> post != null).toList();
    }

    // FULLTEXT BOOLEAN MODE 의 phrase 구문(따옴표)이 깨지지 않도록 제거한다.
    private String sanitizeKeyword(String keyword) {
        return keyword.replace("\"", "");
    }
}
