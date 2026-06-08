package kakao.bootcamp.fullstack.api.dto.response;

import java.util.List;

public record PostSummaryPageResDto(
        List<PostSummaryResDto> data,
        Long nextCursor,
        boolean hasNext
) {
}
