package kakao.bootcamp.fullstack.api.service.search;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchIndex;
import kakao.bootcamp.fullstack.api.repository.search.PostSearchOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostSearchOutboxProcessor {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 20;
    private static final long BACKOFF_CAP_SECONDS = 300;

    private final PostSearchOutboxRepository postSearchOutboxRepository;
    private final PostRepository postRepository;
    private final PostSearchIndex postSearchIndex;
    private final Clock clock;

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void process() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<PostSearchOutbox> batch = postSearchOutboxRepository.findProcessable(now, BATCH_SIZE);
        if (batch.isEmpty()) {
            return;
        }
        // 같은 글의 요청 여러 건은 한 번만 반영하면 된다 — 어차피 현재 상태를 읽어 덮어쓴다.
        Map<Long, List<PostSearchOutbox>> byPostId =
                batch.stream()
                        .collect(
                                Collectors.groupingBy(
                                        PostSearchOutbox::getPostId,
                                        LinkedHashMap::new,
                                        Collectors.toList()));
        byPostId.forEach(
                (postId, rows) -> {
                    try {
                        syncPost(postId);
                        rows.forEach(PostSearchOutbox::markDone);
                    } catch (RuntimeException e) {
                        handleFailure(postId, rows, now, e);
                    }
                });
    }

    private void syncPost(Long postId) {
        postRepository
                .findById(postId)
                .filter(post -> !post.isDeleted())
                .ifPresentOrElse(postSearchIndex::index, () -> postSearchIndex.delete(postId));
    }

    private void handleFailure(
            Long postId, List<PostSearchOutbox> rows, LocalDateTime now, RuntimeException e) {
        for (PostSearchOutbox row : rows) {
            if (row.getRetryCount() + 1 >= MAX_RETRY) {
                row.markFailed();
                log.error("색인 동기화 재시도 한도 초과: outboxId={}, postId={}", row.getId(), row.getPostId());
            } else {
                row.retryLater(now.plusSeconds(backoffSeconds(row.getRetryCount())));
            }
        }
        log.warn("색인 동기화 실패: postId={}", postId, e);
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(1L << Math.min(retryCount, 9), BACKOFF_CAP_SECONDS);
    }
}
