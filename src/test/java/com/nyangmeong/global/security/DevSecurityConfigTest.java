package com.nyangmeong.global.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발용 설정은 prod가 아닐 때만 모든 요청을 열고, prod에서는 기본 보안이 막는지 검사한다.
 */
class DevSecurityConfigTest {

    @RestController
    static class PingController {

        @GetMapping("/test/ping")
        String get() {
            return "ok";
        }

        @PostMapping("/test/ping")
        String post() {
            return "ok";
        }
    }

    @Nested
    @WebMvcTest
    @Import({DevSecurityConfig.class, PingController.class})
    class 기본_프로필 {

        @Autowired
        MockMvc mockMvc;

        @Test
        void 로그인_없이_조회할_수_있다() throws Exception {
            mockMvc.perform(get("/test/ping").header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isOk());
        }

        @Test
        void CSRF_토큰_없이_변경_요청을_보낼_수_있다() throws Exception {
            mockMvc.perform(post("/test/ping").header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @WebMvcTest
    @ActiveProfiles("prod")
    @Import({DevSecurityConfig.class, PingController.class})
    class prod_프로필 {

        @Autowired
        MockMvc mockMvc;

        @Test
        void 로그인_없이_조회하면_401() throws Exception {
            mockMvc.perform(get("/test/ping").header("X-Requested-With", "XMLHttpRequest"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
