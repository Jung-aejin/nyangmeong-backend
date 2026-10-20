package com.nyangmeong.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.error.GlobalExceptionHandler;
import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.AuthMemberMapper;
import com.nyangmeong.global.security.SecurityConfig;
import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;
import com.nyangmeong.global.security.domain.AuthMember;
import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.domain.MemberStatus;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtProperties;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

/**
 * [검사 - 개발용 토큰 발급]
 * - prod 프로필에서는 경로 없음
 */
class DevTokenControllerTest {

    @Import({AuthService.class, FieldCryptoService.class, CryptoProperties.class, SecurityConfig.class,
            JwtTokenProvider.class, JwtProperties.class, TokenCookies.class, ErrorResponseWriter.class,
            RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, SensitiveDataMasker.class,
            GlobalExceptionHandler.class})
    abstract static class Base {

        @Autowired
        MockMvc mockMvc;

        @MockitoBean
        AuthMemberMapper authMemberMapper;

        MockHttpServletRequestBuilder issue(long mbrSn) {
            return post("/api/auth/dev-tokens")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mbrSn\":" + mbrSn + "}");
        }
    }

    @Nested
    @WebMvcTest(controllers = DevTokenController.class)
    class 기본_프로필 extends Base {

        @Test
        void 있는_회원이면_access_refresh_쿠키_두_개를_내려준다() throws Exception {
            AuthMember member = new AuthMember();
            member.setMbrSn(3L);
            member.setMbrSttsCd(MemberStatus.NORMAL.getCode());
            member.setMbrAuthrtCd(MemberAuthority.USER.getCode());
            when(authMemberMapper.selectForAuth(3L)).thenReturn(member);

            mockMvc.perform(issue(3L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(header().stringValues("Set-Cookie", Matchers.hasItems(
                            Matchers.allOf(Matchers.startsWith("access_token="), Matchers.containsString("HttpOnly"),
                                    Matchers.containsString("Path=/")),
                            Matchers.allOf(Matchers.startsWith("refresh_token="),
                                    Matchers.containsString("Path=/api/auth")))));
        }

        @Test
        void 없는_회원이면_404_MEMBER_NOT_FOUND다() throws Exception {
            when(authMemberMapper.selectForAuth(99L)).thenReturn(null);

            mockMvc.perform(issue(99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
        }

        @Test
        void 회원_번호가_없으면_400_INVALID_INPUT_VALUE다() throws Exception {
            mockMvc.perform(post("/api/auth/dev-tokens")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                    .andExpect(jsonPath("$.data[0].field").value("mbrSn"));
        }
    }

    @Nested
    @WebMvcTest(controllers = DevTokenController.class)
    @ActiveProfiles("prod")
    class prod_프로필 extends Base {

        @Test
        void 빈이_없어_404_NOT_FOUND다() throws Exception {
            mockMvc.perform(issue(3L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }
}
