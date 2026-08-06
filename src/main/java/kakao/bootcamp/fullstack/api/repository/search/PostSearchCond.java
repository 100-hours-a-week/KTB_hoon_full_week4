package kakao.bootcamp.fullstack.api.repository.search;

import java.time.LocalDateTime;
import kakao.bootcamp.fullstack.api.domain.post.MeetingType;
import kakao.bootcamp.fullstack.api.domain.post.PostCategory;
import kakao.bootcamp.fullstack.api.domain.post.RecruitStatus;

public record PostSearchCond(
        String keyword,
        PostCategory category,
        MeetingType meetingType,
        RecruitStatus recruitStatus,
        String sido,
        String sigungu,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        Long cursor,
        Long size) {}
