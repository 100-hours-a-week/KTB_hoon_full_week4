package kakao.bootcamp.fullstack.global.security.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JjwtProvider implements JwtProvider {

    private final JwtProperties jwtProperties;

    @Override
    public String createAccessToken(Long memberId, String email, Role role) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(toDate(now))
                .expiration(toDate(expiration))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public void validateToken(String token) {
        try {
            parseClaims(token);
        } catch (ExpiredJwtException e) {
            throw new UnauthorizedException(AuthErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    @Override
    public Long getMemberId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    @Override
    public String getEmail(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    @Override
    public String getJti(String token) {
        Claims claims = parseClaims(token);
        return claims.getId();
    }

    @Override
    public Role getRole(String token) {
        Claims claims = parseClaims(token);
        String role = claims.get("role", String.class);
        return Role.valueOf(role);
    }

    @Override
    public long getExpirationMillis(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration().getTime();
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

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}