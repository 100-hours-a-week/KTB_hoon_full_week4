package kakao.bootcamp.fullstack.member.fixture.dto;

import kakao.bootcamp.fullstack.api.dto.request.ProfileUpdateReqDto;

public class ProfileUpdateReqDtoFixture {

    private static final String DEFAULT_NICKNAME = "nick";
    private static final String DEFAULT_IMAGE_URL = "new-url";

    public static ProfileUpdateReqDto valid() {
        return new ProfileUpdateReqDto(DEFAULT_NICKNAME, DEFAULT_IMAGE_URL);
    }

    public static ProfileUpdateReqDto withNickname(String nickname) {
        return new ProfileUpdateReqDto(nickname, DEFAULT_IMAGE_URL);
    }
}
