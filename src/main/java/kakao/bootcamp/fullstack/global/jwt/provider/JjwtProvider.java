package kakao.bootcamp.fullstack.global.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.UUID;
import kakao.bootcamp.fullstack.global.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JjwtProvider implements JwtProvider {

    private final JwtProperties jwtProperties;

    @Override
    public String createAccessToken(Long memberId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    @Override
    public String getJti(String token) {
        Claims claims = parseClaims(token);
        return claims.getId();
    }

    @Override
    public long getExpirationMillis(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime();
    }

    public String getEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8)
        );
    }
}