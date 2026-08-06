package kakao.bootcamp.fullstack.api.repository.search;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.post.Post;

public interface SearchRepository {
    List<Post> searchPostPage(String keyword, Long cursor, Long size);
}
