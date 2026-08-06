package kakao.bootcamp.fullstack.api.dto.response;

import kakao.bootcamp.fullstack.api.domain.post.Address;

public record AddressResDto(String sido, String sigungu, String eupmyeondong, String detail) {

    public static AddressResDto from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResDto(
                address.getSido(),
                address.getSigungu(),
                address.getEupmyeondong(),
                address.getDetail());
    }
}
