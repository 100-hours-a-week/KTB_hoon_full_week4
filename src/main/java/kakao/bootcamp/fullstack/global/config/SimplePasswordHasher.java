package kakao.bootcamp.fullstack.global.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import kakao.bootcamp.fullstack.global.constants.PasswordConstants;

public class SimplePasswordHasher implements PasswordHasher {

    @Override
    public String hash(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(PasswordConstants.HASH_ALGORITHM);
            byte[] hashedBytes = digest.digest(rawPassword.getBytes());
            return HexFormat.of().formatHex(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("비밀번호 해시 처리 중 알고리즘을 찾을 수 없습니다.", e);
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
