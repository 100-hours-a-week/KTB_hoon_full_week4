package kakao.bootcamp.fullstack.api.repository.search;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;

public interface SearchRepository {
    List<Post> searchPostPage(String keyword, PostCategory category, Long cursor, Long size);
}
