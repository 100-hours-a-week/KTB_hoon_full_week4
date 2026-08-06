package kakao.bootcamp.fullstack.api.domain.post;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostCategory {
    EXERCISE("운동/스포츠"),
    STUDY("스터디"),
    HOBBY("취미"),
    GAME("게임/오락"),
    FOOD("맛집/모임"),
    VOLUNTEER("봉사/나눔"),
    ETC("기타");

    private final String label;
}
