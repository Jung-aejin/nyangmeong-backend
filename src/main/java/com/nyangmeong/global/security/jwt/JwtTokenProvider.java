package com.nyangmeong.global.security.jwt;

import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * [JWT 발급/검증]
 * - subject : 회원 일련번호(MBR_SN)
 * - Access Token : API 인증용 / Refresh Token : 재발급 전용
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final JwtProperties properties;

    private SecretKey signingKey;

    @PostConstruct
    public void initializeKey() {
        String secret = properties.getSecretBase64();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret-base64 값이 없습니다. 디스코드의 application.properties를 확인해 주세요.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("app.jwt.secret-base64 값이 Base64가 아닙니다.", e);
        }
        if (keyBytes.length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret-base64는 Base64로 풀었을 때 32바이트 이상이어야 합니다.");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long mbrSn) {
        return create(mbrSn, properties.getAccessTokenExpiryMs());
    }

    public String createRefreshToken(Long mbrSn) {
        return create(mbrSn, properties.getRefreshTokenExpiryMs());
    }

    /**
     * 토큰에서 회원 일련번호 추출
     */
    public Long parseMbrSn(String token) {
        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            /** subject 형식 불일치 */
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String create(Long mbrSn, long expiryMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(mbrSn))
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryMs))
                .signWith(signingKey)
                .compact();
    }
}
