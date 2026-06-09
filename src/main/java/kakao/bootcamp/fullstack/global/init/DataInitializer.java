package kakao.bootcamp.fullstack.global.init;

import java.util.List;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;
import kakao.bootcamp.fullstack.global.hasher.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostDraftRepository postDraftRepository;
    private final PasswordHasher passwordHasher;

    @Override
    public void run(String... args) {
        String encodedPassword = passwordHasher.hash("Password1!");

        Member alice = Member.create("alice@example.com", encodedPassword, "앨리스", "https://picsum.photos/seed/alice/200");
        Member bob = Member.create("bob@example.com", encodedPassword, "밥", "https://picsum.photos/seed/bob/200");
        Member carol = Member.create("carol@example.com", encodedPassword, "캐롤", "https://picsum.photos/seed/carol/200");
        Member dave = Member.create("dave@example.com", encodedPassword, "데이브", "https://picsum.photos/seed/dave/200");
        Member donghoon = Member.create("leedonghoon@example.com", passwordHasher.hash("Test1234!"), "donghoon", "https://cdn.example.com/profile.jpg");
        List<Member> members = List.of(alice, bob, carol, dave, donghoon);
        members.forEach(memberRepository::save);

        Post post1 = Post.create(alice,
                "스프링 부트로 시작하는 백엔드",
                "오늘은 스프링 부트 프로젝트를 처음 세팅해봤어요. 의존성 설정이 생각보다 간단했습니다.",
                "https://picsum.photos/seed/post1/600/400");
        Post post2 = Post.create(bob,
                "JWT 인증 흐름 정리",
                "Access/Refresh 토큰을 분리하고 블랙리스트로 로그아웃을 처리한 경험을 공유합니다.",
                "https://picsum.photos/seed/post2/600/400");
        Post post3 = Post.create(carol,
                "도메인 주도 설계 입문기",
                "엔티티와 값 객체의 차이를 이해하는 데 시간이 좀 걸렸네요. 추천 자료 있을까요?",
                "https://picsum.photos/seed/post3/600/400");
        Post post4 = Post.create(alice,
                "인메모리 저장소로 빠른 프로토타이핑",
                "DB 붙이기 전 ConcurrentHashMap만으로도 충분히 검증이 가능합니다.",
                "https://picsum.photos/seed/post4/600/400");
        Post post5 = Post.create(dave,
                "프론트엔드 상태관리 비교",
                "Redux, Zustand, Recoil을 작은 프로젝트에 적용해본 후기입니다.",
                "https://picsum.photos/seed/post5/600/400");
        Post post6 = Post.create(bob,
                "테스트 코드, 어디까지 작성하나요?",
                "단위 테스트와 통합 테스트의 균형이 늘 고민입니다. 다들 어떻게 하시나요?",
                "https://picsum.photos/seed/post6/600/400");
        List<Post> posts = List.of(post1, post2, post3, post4, post5, post6);
        posts.forEach(postRepository::save);

        List<Comment> comments = List.of(
                Comment.create(post1.getId(), bob, "저도 어제 세팅했는데 한 번에 잘 되더라구요!"),
                Comment.create(post1.getId(), carol, "스타터 의존성 덕분에 진짜 편해진 듯해요."),
                Comment.create(post2.getId(), alice, "Refresh 토큰은 어디에 저장하시나요?"),
                Comment.create(post2.getId(), dave, "쿠키(HttpOnly) 추천드립니다."),
                Comment.create(post2.getId(), carol, "블랙리스트 구현 방식 자세히 듣고 싶어요."),
                Comment.create(post3.getId(), bob, "에릭 에반스의 DDD 책 추천드려요."),
                Comment.create(post4.getId(), dave, "PoC 단계에서는 정말 좋은 선택이죠."),
                Comment.create(post5.getId(), alice, "Zustand가 러닝커브 면에서는 제일 편했어요."),
                Comment.create(post5.getId(), bob, "저는 작은 프로젝트엔 Recoil을 자주 씁니다."),
                Comment.create(post6.getId(), carol, "도메인 레이어는 단위, 컨트롤러는 통합으로 가는 편입니다.")
        );
        comments.forEach(commentRepository::save);

        List<PostDraft> drafts = List.of(
                PostDraft.create(donghoon.getId(),
                        "auto-save 시작한 글",
                        null,
                        null),
                PostDraft.create(donghoon.getId(),
                        "JWT 리프레시 토큰 운영 후기",
                        "Access는 짧게, Refresh는 HttpOnly 쿠키로 분리한 뒤로 운영 비용이 줄었습니다. 다음 글에서 블랙리스트 정책 정리 예정.",
                        null),
                PostDraft.create(donghoon.getId(),
                        "ConcurrentHashMap 기반 인메모리 저장소 설계",
                        "초기 프로토타입에서는 ConcurrentHashMap 하나로 충분합니다. ID 생성기는 AtomicLong으로 분리해두면 추후 DB 전환 시 변경 폭을 좁힐 수 있어요.",
                        "https://picsum.photos/seed/draft3/600/400")
        );
        drafts.forEach(postDraftRepository::save);

        log.info("[DataInitializer] seeded members={}, posts={}, comments={}, drafts={}",
                members.size(), posts.size(), comments.size(), drafts.size());
    }
}
