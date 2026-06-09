package kakao.bootcamp.fullstack.global.hasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import kakao.bootcamp.fullstack.global.constants.PasswordConstants;
import kakao.bootcamp.fullstack.global.exception.BusinessException;
import kakao.bootcamp.fullstack.global.exception.code.CommonErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SimplePasswordEncoder implements PasswordEncoder {

    @Override
    public String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(PasswordConstants.HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(rawPassword.getBytes());
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return hash(rawPassword).equals(hashedPassword);
    }
}
