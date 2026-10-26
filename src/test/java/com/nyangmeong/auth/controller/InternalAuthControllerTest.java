package com.nyangmeong.auth.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.filter.InternalKeyFilter;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.http.Cookie;

/**
 * [검사 - GET /internal/auth/check]
 * - 실제 필터 체인, 회원 조회만 Mock. 키는 테스트 설정(config/application.yml)의 app.internal.inbound-key
 */
@WebMvcTest(controllers = InternalAuthController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class, TokenCookies.class,
        ErrorResponseWriter.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class,
        SensitiveDataMasker.class, GlobalExceptionHandler.class})
class InternalAuthControllerTest {

    private static final String PATH = "/internal/auth/check";
    private static final String KEY = "test-internal-inbound-key";
    /** 로그인 세션 번호 */
    private static final Long SID = 5L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    AuthMemberMapper authMemberMapper;

    private static AuthMember member(String status) {
        AuthMember member = new AuthMember();
        member.setMbrSn(1L);
        member.setMbrSttsCd(status);
        member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
        return member;
    }

    private Cookie accessCookie() {
        return new Cookie(TokenCookies.ACCESS_TOKEN, jwtTokenProvider.createAccessToken(1L, SID));
    }

    @Test
    void 키가_없으면_쿠키가_있어도_401_INVALID_INTERNAL_KEY이고_회원을_조회하지_않는다() throws Exception {
        mockMvc.perform(get(PATH).cookie(accessCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_KEY"));
        mockMvc.perform(get(PATH).cookie(accessCookie()).header(InternalKeyFilter.HEADER, "wrong-key"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_INTERNAL_KEY"));

        verifyNoInteractions(authMemberMapper);
    }

    @Test
    void 키가_맞고_쿠키가_없으면_401_UNAUTHORIZED다() throws Exception {
        mockMvc.perform(get(PATH).header(InternalKeyFilter.HEADER, KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 키가_맞고_정지_회원이면_403_MEMBER_INACTIVE와_쿠키_삭제_헤더다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.SUSPENDED.getCode()));

        mockMvc.perform(get(PATH).cookie(accessCookie()).header(InternalKeyFilter.HEADER, KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("MEMBER_INACTIVE"))
                .andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
                        Matchers.containsString("access_token=;"),
                        Matchers.containsString("refresh_token=;"))));
    }

    @Test
    void 키와_로그인이_정상이면_회원_번호와_권한만_돌려준다() throws Exception {
        when(authMemberMapper.selectForSession(1L, SID)).thenReturn(member(MemberStatus.NORMAL.getCode()));

        mockMvc.perform(get(PATH).cookie(accessCookie()).header(InternalKeyFilter.HEADER, KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.data.mbrSn").value(1))
                .andExpect(jsonPath("$.data.mbrAuthrtCd").value("USRC0004"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
