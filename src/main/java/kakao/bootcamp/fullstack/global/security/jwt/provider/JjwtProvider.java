package kakao.bootcamp.fullstack.global.security.jwt.provider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import kakao.bootcamp.fullstack.api.domain.auth.AuthErrorCode;
import kakao.bootcamp.fullstack.api.domain.member.Role;
import kakao.bootcamp.fullstack.global.exception.UnauthorizedException;
import kakao.bootcamp.fullstack.global.security.dto.AccessTokenPayload;
import kakao.bootcamp.fullstack.global.security.jwt.properties.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JjwtProvider implements JwtProvider {

    private SecretKey key;
    private final JwtProperties jwtProperties;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
    }

    @Override
    public String createAccessToken(Long memberId, String email, Role role, String familyId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiration = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim("email", email)
                .claim("role", role)
                .claim("fid", familyId)
                .issuedAt(toDate(now))
                .expiration(toDate(expiration))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public AccessTokenPayload parseAccessToken(String token) {
        try {
            Claims claims =
                    Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return new AccessTokenPayload(
                    Long.valueOf(claims.getSubject()),
                    claims.get("email", String.class),
                    Role.valueOf(claims.get("role", String.class)),
                    claims.getId(),
                    claims.get("fid", String.class),
                    claims.getExpiration().getTime());
        } catch (ExpiredJwtException e) {
            // 만료는 재발급으로 자연 복구되는 정상 이벤트 → 소음 방지를 위해 debug
            log.debug("AT 만료: 재발급 대상.");
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 서명 불일치·구조 손상 등은 위변조 가능성 → warn
            log.warn("AT 검증 실패(위변조 의심): {}", e.getMessage());
            throw new UnauthorizedException(AuthErrorCode.INVALID_TOKEN);
        }
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
