package com.nyangmeong.global.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

/**
 * [JWT 발급/검증]
 * - subject : 회원 일련번호(MBR_SN)
 * - Access Token : API 인증용 / Refresh Token : 재발급 전용
 * - Spring Security JwtEncoder/JwtDecoder(Nimbus), 알고리즘은 키 길이로 결정
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final int MINIMUM_SECRET_BYTES = 32;

    private final JwtProperties properties;

    private MacAlgorithm algorithm;

    private JwtEncoder encoder;

    private JwtDecoder decoder;

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
        algorithm = algorithmFor(keyBytes.length);
        SecretKey signingKey = new SecretKeySpec(keyBytes, algorithm.getName());
        /** kid 없는 키 저장소 -> 헤더는 alg 만 */
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(signingKey));
        NimbusJwtDecoder nimbusDecoder = NimbusJwtDecoder.withSecretKey(signingKey).macAlgorithm(algorithm).build();
        /** 기본 만료 여유 60초 -> 0초 */
        nimbusDecoder.setJwtValidator(new JwtTimestampValidator(Duration.ZERO));
        decoder = nimbusDecoder;
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
        Jwt jwt;
        try {
            jwt = decoder.decode(token);
        } catch (JwtValidationException e) {
            /** 등록한 검증기가 만료 검사뿐 -> 만료 */
            throw new CustomException(ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            /** subject 형식 불일치 */
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String create(Long mbrSn, long expiryMs) {
        /** JWT 시각은 초 단위 */
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plusMillis(expiryMs).truncatedTo(ChronoUnit.SECONDS);
        if (!expiresAt.isAfter(issuedAt)) {
            /** Nimbus 는 exp > iat 를 요구 (0 이하 만료 시간 대응) */
            issuedAt = expiresAt.minusSeconds(1);
        }
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(mbrSn))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(algorithm).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * 키 길이 -> HMAC 알고리즘 (HS256=32, HS384=48, HS512=64바이트 이상)
     */
    private static MacAlgorithm algorithmFor(int keyLength) {
        if (keyLength >= 64) {
            return MacAlgorithm.HS512;
        }
        if (keyLength >= 48) {
            return MacAlgorithm.HS384;
        }
        return MacAlgorithm.HS256;
    }
}
