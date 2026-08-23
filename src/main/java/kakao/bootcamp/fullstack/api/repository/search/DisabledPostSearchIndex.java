package kakao.bootcamp.fullstack.api.repository.search;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public class DisabledPostSearchIndex implements PostSearchIndex {

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public List<Long> searchIds(PostSearchCond cond) {
        throw new IllegalStateException("OpenSearch 가 비활성 상태다");
    }

    @Override
    public void index(Post post) {}

    @Override
    public void delete(Long postId) {}
}
