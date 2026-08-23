package kakao.bootcamp.fullstack.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.search.OutboxStatus;
import kakao.bootcamp.fullstack.api.domain.search.PostSearchOutbox;
import kakao.bootcamp.fullstack.api.service.search.PostSearchOutboxProcessor;
import kakao.bootcamp.fullstack.global.fake.MutableClock;
import kakao.bootcamp.fullstack.member.fixture.MemberFixture;
import kakao.bootcamp.fullstack.post.fake.FakePostRepository;
import kakao.bootcamp.fullstack.post.fixture.PostFixture;
import kakao.bootcamp.fullstack.search.fake.FakePostSearchIndex;
import kakao.bootcamp.fullstack.search.fake.FakePostSearchOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class PostSearchOutboxProcessorTest {

    private static final Long WRITER_ID = 1L;
    private static final Long POST_ID = 10L;

    private final FakePostSearchOutboxRepository outboxRepository =
            new FakePostSearchOutboxRepository();
    private final FakePostRepository postRepository = new FakePostRepository();
    private final FakePostSearchIndex postSearchIndex = new FakePostSearchIndex();
    private final MutableClock clock = new MutableClock(Instant.parse("2026-08-23T00:00:00Z"));

    private PostSearchOutboxProcessor processor;
    private Post post;

    @BeforeEach
    void setUp() {
        processor =
                new PostSearchOutboxProcessor(
                        outboxRepository, postRepository, postSearchIndex, clock);
        post = PostFixture.post(POST_ID, MemberFixture.activeMember(WRITER_ID));
        postRepository.save(post);
    }

    @Test
    @DisplayName("활성 글 요청을 처리하면 색인하고 DONE으로 바꾼다")
    void indexesActivePost() {
        // given
        outboxRepository.save(PostSearchOutbox.create(POST_ID));

        // when
        processor.process();

        // then
        assertThat(postSearchIndex.indexedPosts()).containsExactly(post);
        assertThat(outboxRepository.rows())
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.DONE));
    }

    @Test
    @DisplayName("삭제된 글이면 색인에서 제거한다")
    void deletesSoftDeletedPost() {
        // given
        post.delete();
        outboxRepository.save(PostSearchOutbox.create(POST_ID));

        // when
        processor.process();

        // then
        assertThat(postSearchIndex.deletedIds()).containsExactly(POST_ID);
        assertThat(outboxRepository.rows())
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.DONE));
    }

    @Test
    @DisplayName("글이 없어도 색인 제거로 처리한다")
    void deletesUnknownPost() {
        // given
        outboxRepository.save(PostSearchOutbox.create(999L));

        // when
        processor.process();

        // then
        assertThat(postSearchIndex.deletedIds()).containsExactly(999L);
        assertThat(outboxRepository.rows())
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.DONE));
    }

    @Test
    @DisplayName("같은 글의 요청 여러 건은 한 번만 색인하고 전부 DONE으로 바꾼다")
    void collapsesDuplicateRequests() {
        // given
        outboxRepository.save(PostSearchOutbox.create(POST_ID));
        outboxRepository.save(PostSearchOutbox.create(POST_ID));

        // when
        processor.process();

        // then
        assertThat(postSearchIndex.indexedPosts()).hasSize(1);
        assertThat(outboxRepository.rows())
                .hasSize(2)
                .allSatisfy(row -> assertThat(row.getStatus()).isEqualTo(OutboxStatus.DONE));
    }

    @Test
    @DisplayName("색인이 실패하면 재시도 시각을 미루고 PENDING으로 남긴다")
    void schedulesRetryOnFailure() {
        // given
        outboxRepository.save(PostSearchOutbox.create(POST_ID));
        postSearchIndex.failWith(new RuntimeException("down"));

        // when
        processor.process();

        // then
        PostSearchOutbox row = outboxRepository.rows().get(0);
        assertThat(row.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(row.getRetryCount()).isEqualTo(1);
        assertThat(row.getNextAttemptAt()).isAfter(LocalDateTime.now(clock));

        // 재시도 시각이 오기 전에는 다시 집지 않는다
        processor.process();
        assertThat(postSearchIndex.indexAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("색인이 복구되면 밀린 요청을 따라잡는다")
    void catchesUpAfterRecovery() {
        // given
        outboxRepository.save(PostSearchOutbox.create(POST_ID));
        postSearchIndex.failWith(new RuntimeException("down"));
        processor.process();

        // when
        postSearchIndex.recover();
        clock.advance(Duration.ofSeconds(2));
        processor.process();

        // then
        assertThat(postSearchIndex.indexedPosts()).containsExactly(post);
        assertThat(outboxRepository.rows().get(0).getStatus()).isEqualTo(OutboxStatus.DONE);
    }

    @Test
    @DisplayName("재시도 한도를 넘기면 FAILED로 격리한다")
    void marksFailedAfterMaxRetries() {
        // given
        outboxRepository.save(PostSearchOutbox.create(POST_ID));
        postSearchIndex.failWith(new RuntimeException("down"));

        // when
        for (int i = 0; i < 20; i++) {
            processor.process();
            clock.advance(Duration.ofSeconds(301));
        }

        // then
        assertThat(outboxRepository.rows().get(0).getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(outboxRepository.rows().get(0).getRetryCount()).isEqualTo(19);
    }
}
