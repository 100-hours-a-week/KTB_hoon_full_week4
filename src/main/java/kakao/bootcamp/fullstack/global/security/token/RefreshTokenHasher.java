package kakao.bootcamp.fullstack.global.security.token;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import kakao.bootcamp.fullstack.global.exception.InternalServerException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenHasher {

    private static final String HASH_ALGORITHM = "SHA-256";

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new InternalServerException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
