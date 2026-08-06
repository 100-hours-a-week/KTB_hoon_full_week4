package kakao.bootcamp.fullstack.post.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.AddressReqDto;
import kakao.bootcamp.fullstack.api.dto.request.PostCreateReqDto;
import kakao.bootcamp.fullstack.post.fixture.PostFixture;

public class PostCreateReqDtoFixture {

    public static PostCreateReqDto valid() {
        return new PostCreateReqDto(
                PostFixture.TITLE,
                PostFixture.CONTENT,
                PostFixture.IMAGE_URL,
                PostFixture.CATEGORY,
                PostFixture.MEETING_TYPE,
                address(),
                PostFixture.PLACE_NAME,
                PostFixture.CAPACITY);
    }

    public static AddressReqDto address() {
        return new AddressReqDto(
                PostFixture.SIDO,
                PostFixture.SIGUNGU,
                PostFixture.EUPMYEONDONG,
                PostFixture.DETAIL);
    }
}
