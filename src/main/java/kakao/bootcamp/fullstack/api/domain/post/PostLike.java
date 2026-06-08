package kakao.bootcamp.fullstack.api.domain.post;

import kakao.bootcamp.fullstack.global.BaseEntity;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    private Long id;
    private Long postId;
    private Long memberId;

    private PostLike(Long postId, Long memberId) {
        this.postId = postId;
        this.memberId = memberId;
    }

    public boolean isNew() {
        return id == null;
    }

    public void assignId(Long id) {
        if (!isNew()) {
            throw new BusinessException(CommonErrorCode.ALREADY_ASSIGNED_ID);
        }
        this.id = id;
    }

    public static PostLike create(Long postId, Long memberId) {
        return new PostLike(postId, memberId);
    }
}