package com.nyangmeong.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.nyangmeong.global.log.SensitiveDataMasker;
import com.nyangmeong.global.security.InternalKeyProperties;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;

import tools.jackson.databind.json.JsonMapper;

/**
 * [검사 - 서버 간 내부 호출 키 필터]
 */
class InternalKeyFilterTest {

    private static final String KEY = "inbound-test-key";

    private final ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(JsonMapper.builder().build(),
            new SensitiveDataMasker());

    private InternalKeyFilter filter(String inboundKey) {
        InternalKeyProperties properties = new InternalKeyProperties();
        properties.setInboundKey(inboundKey);
        return new InternalKeyFilter(properties, errorResponseWriter);
    }

    private MockHttpServletRequest internalRequest(String key) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/auth/check");
        if (key != null) {
            request.addHeader(InternalKeyFilter.HEADER, key);
        }
        return request;
    }

    @Test
    void 설정_키가_비면_어떤_헤더도_401_INVALID_INTERNAL_KEY이고_체인이_끊긴다() throws Exception {
        for (String configured : new String[] {"", "  ", null}) {
            for (String given : new String[] {"", KEY}) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                MockFilterChain chain = new MockFilterChain();

                filter(configured).doFilter(internalRequest(given), response, chain);

                assertThat(response.getStatus()).isEqualTo(401);
                assertThat(response.getContentAsString()).contains("\"code\":\"INVALID_INTERNAL_KEY\"");
                assertThat(chain.getRequest()).isNull();
            }
        }
    }

    @Test
    void 헤더가_없거나_다르면_401_INVALID_INTERNAL_KEY이고_체인이_끊긴다() throws Exception {
        for (String given : new String[] {null, "", "wrong-key", KEY + " "}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter(KEY).doFilter(internalRequest(given), response, chain);

            assertThat(response.getStatus()).isEqualTo(401);
            assertThat(response.getContentType()).startsWith("application/json");
            assertThat(response.getContentAsString()).contains("\"code\":\"INVALID_INTERNAL_KEY\"");
            assertThat(chain.getRequest()).isNull();
        }
    }

    @Test
    void 헤더가_설정_키와_같으면_통과한다() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        filter(KEY).doFilter(internalRequest(KEY), new MockHttpServletResponse(), chain);

        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void 내부_경로가_아니면_키를_검사하지_않는다() throws Exception {
        for (String path : new String[] {"/api/auth/me", "/internals", "/api/internal/x"}) {
            MockFilterChain chain = new MockFilterChain();

            filter("").doFilter(new MockHttpServletRequest("GET", path), new MockHttpServletResponse(), chain);

            assertThat(chain.getRequest()).isNotNull();
        }
    }
}
