package kakao.bootcamp.fullstack.post.fixture;

import kakao.bootcamp.fullstack.api.domain.member.Member;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.Post;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;

public class PostFixture {

    public static final String TITLE = "제목";
    public static final String CONTENT = "본문";
    public static final String IMAGE_URL = "https://cdn.example.com/post.png";
    public static final PostCategory CATEGORY = PostCategory.EXERCISE;
    public static final MeetingType MEETING_TYPE = MeetingType.OFFLINE;
    public static final String SIDO = "서울특별시";
    public static final String SIGUNGU = "강남구";
    public static final String EUPMYEONDONG = "대치동";
    public static final String DETAIL = null;
    public static final String PLACE_NAME = "대치중학교 운동장";
    public static final Integer CAPACITY = 6;

    public static Address address() {
        return Address.of(SIDO, SIGUNGU, EUPMYEONDONG, DETAIL);
    }

    public static Post post(Member writer) {
        return Post.create(
                writer,
                TITLE,
                CONTENT,
                IMAGE_URL,
                CATEGORY,
                MEETING_TYPE,
                address(),
                PLACE_NAME,
                CAPACITY);
    }

    public static Post post(Long id, Member writer) {
        Post post = post(writer);
        post.assignId(id);
        return post;
    }

    public static Post onlinePost(Member writer) {
        return Post.create(
                writer,
                TITLE,
                CONTENT,
                IMAGE_URL,
                CATEGORY,
                MeetingType.ONLINE,
                null,
                PLACE_NAME,
                CAPACITY);
    }
}
