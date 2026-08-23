package kakao.bootcamp.fullstack.api.repository.search;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface PostSearchIndex {

    boolean isEnabled();

    List<Long> searchIds(PostSearchCond cond);

    void index(Post post);

    void delete(Long postId);
}
