package kakao.bootcamp.fullstack.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import kakao.bootcamp.fullstack.api.domain.post.Address;
import kakao.bootcamp.fullstack.global.exception.code.ValidationCode;

public record AddressReqDto(
        @NotBlank(message = ValidationCode.SIDO_REQUIRED) String sido,
        @NotBlank(message = ValidationCode.SIGUNGU_REQUIRED) String sigungu,
        @NotBlank(message = ValidationCode.EUPMYEONDONG_REQUIRED) String eupmyeondong,
        String detail) {

    public Address toAddress() {
        return Address.of(sido, sigungu, eupmyeondong, detail);
    }
}
