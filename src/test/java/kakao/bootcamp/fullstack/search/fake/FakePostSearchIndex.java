package kakao.bootcamp.fullstack.search.fake;

import java.util.ArrayList;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchCond;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;

public class FakePostSearchIndex implements PostSearchIndex {

    private final List<Post> indexedPosts = new ArrayList<>();
    private final List<Long> deletedIds = new ArrayList<>();
    private List<Long> searchResultIds = List.of();

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public List<Long> searchIds(PostSearchCond cond) {
        return searchResultIds;
    }

    @Override
    public void index(Post post) {
        indexedPosts.add(post);
    }

    @Override
    public void delete(Long postId) {
        deletedIds.add(postId);
    }

    public void givenSearchResultIds(List<Long> ids) {
        this.searchResultIds = ids;
    }

    public List<Post> indexedPosts() {
        return indexedPosts;
    }

    public List<Long> deletedIds() {
        return deletedIds;
    }
}
