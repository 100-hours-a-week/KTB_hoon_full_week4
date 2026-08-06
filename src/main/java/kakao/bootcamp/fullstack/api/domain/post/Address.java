package kakao.bootcamp.fullstack.api.domain.post;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Address {

    @Column(length = 20)
    private String sido;

    @Column(length = 20)
    private String sigungu;

    @Column(length = 20)
    private String eupmyeondong;

    @Column(length = 100)
    private String detail;

    private Address(String sido, String sigungu, String eupmyeondong, String detail) {
        this.sido = sido;
        this.sigungu = sigungu;
        this.eupmyeondong = eupmyeondong;
        this.detail = detail;
    }

    public static Address of(String sido, String sigungu, String eupmyeondong, String detail) {
        return new Address(sido, sigungu, eupmyeondong, detail);
    }
}
