package kakao.bootcamp.fullstack.global.init;

import java.util.ArrayList;
import java.util.List;
import kakao.bootcamp.fullstack.api.domain.comment.Comment;
import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.PostLike;
import kakao.bootcamp.fullstack.api.domain.post.PostViewLog;
import kakao.bootcamp.fullstack.api.domain.post_draft.PostDraft;
import kakao.bootcamp.fullstack.api.repository.comment.CommentRepository;
import kakao.bootcamp.fullstack.api.repository.member.MemberRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostLikeRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostRepository;
import kakao.bootcamp.fullstack.api.repository.post.PostViewLogRepository;
import kakao.bootcamp.fullstack.api.repository.post_draft.PostDraftRepository;
import kakao.bootcamp.fullstack.global.security.hasher.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class JpaDataInitializer implements CommandLineRunner {

    private static final String SEED_GUARD_EMAIL = "alice@example.com";

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostViewLogRepository postViewLogRepository;
    private final CommentRepository commentRepository;
    private final PostDraftRepository postDraftRepository;
    private final PasswordHasher passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.existsByEmailIncludingDeleted(SEED_GUARD_EMAIL)) {
            log.info("[JpaDataInitializer] skip — already seeded");
            return;
        }

        String encodedPassword = passwordEncoder.hash("Password1!");

        Member alice =
                Member.create(
                        SEED_GUARD_EMAIL,
                        encodedPassword,
                        "앨리스",
                        "https://picsum.photos/seed/alice/200");
        Member bob =
                Member.create(
                        "bob@example.com",
                        encodedPassword,
                        "밥",
                        "https://picsum.photos/seed/bob/200");
        Member carol =
                Member.create(
                        "carol@example.com",
                        encodedPassword,
                        "캐롤",
                        "https://picsum.photos/seed/carol/200");
        Member dave =
                Member.create(
                        "dave@example.com",
                        encodedPassword,
                        "데이브",
                        "https://picsum.photos/seed/dave/200");
        Member donghoon =
                Member.create(
                        "leedonghoon@example.com",
                        passwordEncoder.hash("Test1234!"),
                        "donghoon",
                        "https://cdn.example.com/profile.jpg");
        List<Member> members = List.of(alice, bob, carol, dave, donghoon);
        members.forEach(memberRepository::save);

        Post post1 =
                Post.create(
                        alice,
                        "주말 농구 같이 하실 분 구해요",
                        "일요일 오전에 동네 농구장에서 반코트 게임 합니다. 두세 명만 더 오면 딱 좋아요. 초보도 환영합니다.",
                        "https://picsum.photos/seed/post1/600/400",
                        PostCategory.EXERCISE,
                        MeetingType.OFFLINE,
                        Address.of("서울특별시", "강남구", "대치동", null),
                        "대치중학교 운동장",
                        6);
        Post post2 =
                Post.create(
                        bob,
                        "자바 알고리즘 스터디 모집합니다",
                        "주 2회 온라인으로 코딩테스트 문제 풀고 리뷰해요. 꾸준히 하실 분만. 노쇼 벌금 있습니다.",
                        "https://picsum.photos/seed/post2/600/400",
                        PostCategory.STUDY,
                        MeetingType.ONLINE,
                        null,
                        "온라인(디스코드)",
                        5);
        Post post3 =
                Post.create(
                        carol,
                        "보드게임 모임 새 멤버 구해요",
                        "매주 토요일 오후에 모여 보드게임 합니다. 룰 몰라도 알려드려요. 처음 오시는 분도 편하게 오세요.",
                        "https://picsum.photos/seed/post3/600/400",
                        PostCategory.GAME,
                        MeetingType.OFFLINE,
                        Address.of("서울특별시", "마포구", "서교동", null),
                        "홍대입구역 보드게임카페",
                        8);
        Post post4 =
                Post.create(
                        alice,
                        "한강 러닝 크루 같이 뛰어요",
                        "화요일 저녁 7시 여의도 한강공원에서 가볍게 5km 뜁니다. 러닝 끝나고 스트레칭도 같이 해요.",
                        "https://picsum.photos/seed/post4/600/400",
                        PostCategory.EXERCISE,
                        MeetingType.OFFLINE,
                        Address.of("서울특별시", "영등포구", "여의도동", null),
                        "여의도한강공원 물빛광장",
                        10);
        Post post5 =
                Post.create(
                        dave,
                        "동네 맛집 탐방 모임 구합니다",
                        "격주로 숨은 맛집 찾아다니며 같이 먹어요. 이번엔 성수동 골목입니다. 먹는 거 좋아하면 누구나 환영.",
                        "https://picsum.photos/seed/post5/600/400",
                        PostCategory.FOOD,
                        MeetingType.OFFLINE,
                        Address.of("서울특별시", "성동구", "성수동", null),
                        "성수역 3번 출구",
                        4);
        Post post6 =
                Post.create(
                        bob,
                        "주말 유기견 산책 봉사 함께 가요",
                        "토요일 아침 보호소에서 강아지 산책과 청소를 돕습니다. 카풀 가능해요. 강아지 좋아하는 분 환영합니다.",
                        "https://picsum.photos/seed/post6/600/400",
                        PostCategory.VOLUNTEER,
                        MeetingType.OFFLINE,
                        Address.of("서울특별시", "은평구", "응암동", null),
                        "응암역 2번 출구",
                        6);
        List<Post> posts = new ArrayList<>(List.of(post1, post2, post3, post4, post5, post6));
        posts.addAll(createSamplePosts(members));
        posts.forEach(postRepository::save);

        List<Comment> comments =
                List.of(
                        Comment.create(post1, bob, "저도 어제 세팅했는데 한 번에 잘 되더라구요!"),
                        Comment.create(post1, carol, "스타터 의존성 덕분에 진짜 편해진 듯해요."),
                        Comment.create(post2, alice, "Refresh 토큰은 어디에 저장하시나요?"),
                        Comment.create(post2, dave, "쿠키(HttpOnly) 추천드립니다."),
                        Comment.create(post2, carol, "블랙리스트 구현 방식 자세히 듣고 싶어요."),
                        Comment.create(post3, bob, "에릭 에반스의 DDD 책 추천드려요."),
                        Comment.create(post4, dave, "PoC 단계에서는 정말 좋은 선택이죠."),
                        Comment.create(post5, alice, "Zustand가 러닝커브 면에서는 제일 편했어요."),
                        Comment.create(post5, bob, "저는 작은 프로젝트엔 Recoil을 자주 씁니다."),
                        Comment.create(post6, carol, "도메인 레이어는 단위, 컨트롤러는 통합으로 가는 편입니다."));
        comments.forEach(
                comment -> {
                    commentRepository.save(comment);
                    comment.getPost().increaseCommentCount();
                });

        likePost(post1, bob, carol, dave);
        likePost(post2, alice, carol, dave, donghoon);
        likePost(post3, alice, bob);
        likePost(post4, dave);
        likePost(post5, alice, bob, carol);
        likePost(post6, carol, dave);

        viewPost(post1, bob, carol, dave, donghoon);
        viewPost(post2, alice, carol, dave, donghoon);
        viewPost(post3, alice, bob, dave);
        viewPost(post4, bob, dave);
        viewPost(post5, alice, bob, carol, donghoon);
        viewPost(post6, alice, carol, dave);

        List<PostDraft> drafts =
                List.of(
                        PostDraft.create(donghoon, "auto-save 시작한 글", null, null),
                        PostDraft.create(
                                donghoon,
                                "JWT 리프레시 토큰 운영 후기",
                                "Access는 짧게, Refresh는 HttpOnly 쿠키로 분리한 뒤로 운영 비용이 줄었습니다. 다음 글에서 블랙리스트 정책 정리 예정.",
                                null),
                        PostDraft.create(
                                donghoon,
                                "ConcurrentHashMap 기반 인메모리 저장소 설계",
                                "초기 프로토타입에서는 ConcurrentHashMap 하나로 충분합니다. ID 생성기는 AtomicLong으로 분리해두면 추후 DB 전환 시 변경 폭을 좁힐 수 있어요.",
                                "https://picsum.photos/seed/draft3/600/400"),
                        PostDraft.create(
                                donghoon,
                                "Rate Limiter 도배 방지 메모",
                                "1분 3건 fixed window. 임시글 publish도 동일 제한을 받는다.",
                                null),
                        PostDraft.create(
                                donghoon,
                                "조회수 24시간 윈도우 적용 후기",
                                "PostViewLog upsert로 같은 사용자 새로고침 시 조회수 동결. 첫 호출 시점부터 24h 경과해야 재카운트.",
                                "https://picsum.photos/seed/draft5/600/400"),
                        PostDraft.create(
                                alice,
                                "Spring Security 적용 시도기",
                                "필터 체인을 처음 들여다보는 중인데 생각보다 진입장벽이 있네요. 정리해서 올릴 예정입니다.",
                                null),
                        PostDraft.create(
                                bob,
                                "쿠키 vs 로컬스토리지 토큰 저장 비교",
                                "보안 측면에서 어느 쪽이 더 나은지, 실제로 운영하면서 느낀 점을 정리 중.",
                                "https://picsum.photos/seed/draft-bob/600/400"),
                        PostDraft.create(carol, "DDD 전술 패턴 정리 (작성 중)", null, null));
        drafts.forEach(postDraftRepository::save);

        log.info(
                "[JpaDataInitializer] seeded members={}, posts={}, comments={}, drafts={}",
                members.size(),
                posts.size(),
                comments.size(),
                drafts.size());
    }

    private List<Post> createSamplePosts(List<Member> authors) {
        List<Post> posts = new ArrayList<>();
        String[][] data = SamplePosts.DATA;
        for (int i = 0; i < data.length; i++) {
            Member author = authors.get(i % authors.size());
            MeetingType meetingType = MeetingType.valueOf(data[i][3]);
            Address address =
                    meetingType == MeetingType.OFFLINE
                            ? Address.of(data[i][4], data[i][5], data[i][6], null)
                            : null;
            posts.add(
                    Post.create(
                            author,
                            data[i][0],
                            data[i][1],
                            "https://picsum.photos/seed/post" + (i + 7) + "/600/400",
                            PostCategory.valueOf(data[i][2]),
                            meetingType,
                            address,
                            data[i][7],
                            4 + (i % 5)));
        }
        return posts;
    }

    private void likePost(Post post, Member... likers) {
        for (Member liker : likers) {
            postLikeRepository.save(PostLike.create(post, liker));
            post.increaseLikeCount();
        }
    }

    private void viewPost(Post post, Member... viewers) {
        for (Member viewer : viewers) {
            postViewLogRepository.save(PostViewLog.create(post.getId(), viewer.getId()));
            post.increaseViewCount();
        }
    }
}
