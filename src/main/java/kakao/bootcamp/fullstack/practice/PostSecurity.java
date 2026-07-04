package kakao.bootcamp.fullstack.practice;

import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 메서드 기반 인가 확인용 클래스
@Component("postSecurity")
@RequiredArgsConstructor
public class PostSecurity {

    private final PostRepository postRepository;

    public boolean isOwner(Long postId, Long memberId) {
        return postRepository.findActiveById(postId)
                .map(post -> post.getMember().getId().equals(memberId))
                .orElse(false);
    }
}
