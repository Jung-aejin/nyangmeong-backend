package com.nyangmeong.global.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;

/**
 * [검사 - JWT 발급/검증]
 */
class JwtTokenProviderTest {

    private static final byte[] SECRET = "jwt-secret-for-tests-32-bytes-long!!".getBytes();

    private static JwtTokenProvider create(byte[] secret, long accessExpiryMs) {
        JwtProperties properties = new JwtProperties();
        properties.setSecretBase64(Base64.getEncoder().encodeToString(secret));
        properties.setAccessTokenExpiryMs(accessExpiryMs);
        JwtTokenProvider provider = new JwtTokenProvider(properties);
        provider.initializeKey();
        return provider;
    }

    @Test
    void 발급한_Access_토큰의_subject는_회원_번호다() {
        JwtTokenProvider provider = create(SECRET, 60_000);

        assertThat(provider.parseMbrSn(provider.createAccessToken(42L))).isEqualTo(42L);
        assertThat(provider.parseMbrSn(provider.createRefreshToken(42L))).isEqualTo(42L);
    }

    @Test
    void 만료된_토큰은_TOKEN_EXPIRED다() {
        JwtTokenProvider provider = create(SECRET, -1_000);

        assertThatThrownBy(() -> provider.parseMbrSn(provider.createAccessToken(1L)))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED);
    }

    @Test
    void 서명이_다르거나_형식이_틀리면_UNAUTHORIZED다() {
        JwtTokenProvider provider = create(SECRET, 60_000);
        JwtTokenProvider other = create("another-secret-for-tests-32-bytes!!!".getBytes(), 60_000);

        assertErrorCode(() -> provider.parseMbrSn(other.createAccessToken(1L)), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseMbrSn("not.a.jwt"), ErrorCode.UNAUTHORIZED);
        assertErrorCode(() -> provider.parseMbrSn(""), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void subject가_숫자가_아니면_UNAUTHORIZED다() {
        JwtTokenProvider provider = create(SECRET, 60_000);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("jinseong@gmail.com")
                .expiresAt(Instant.now().plusMillis(60_000)).build();
        String token = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(SECRET, "HmacSHA256")))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertErrorCode(() -> provider.parseMbrSn(token), ErrorCode.UNAUTHORIZED);
    }

    @Test
    void 시크릿이_없거나_짧으면_기동_때_실패한다() {
        assertThatThrownBy(() -> create(new byte[0], 1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> create("short".getBytes(), 1)).isInstanceOf(IllegalStateException.class);
    }

    private static void assertErrorCode(Runnable call, ErrorCode expected) {
        assertThatThrownBy(call::run)
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(expected);
    }
}
