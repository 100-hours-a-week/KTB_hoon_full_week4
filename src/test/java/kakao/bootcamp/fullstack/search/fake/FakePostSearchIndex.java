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
    private RuntimeException failure;
    private int indexAttempts = 0;

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
        indexAttempts++;
        if (failure != null) {
            throw failure;
        }
        indexedPosts.add(post);
    }

    @Override
    public void delete(Long postId) {
        if (failure != null) {
            throw failure;
        }
        deletedIds.add(postId);
    }

    public void givenSearchResultIds(List<Long> ids) {
        this.searchResultIds = ids;
    }

    public void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public void recover() {
        this.failure = null;
    }

    public int indexAttempts() {
        return indexAttempts;
    }

    public List<Post> indexedPosts() {
        return indexedPosts;
    }

    public List<Long> deletedIds() {
        return deletedIds;
    }
}
