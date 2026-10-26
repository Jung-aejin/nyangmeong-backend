package com.nyangmeong.global.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.web.filter.OncePerRequestFilter;

import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.security.InternalKeyProperties;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * [서버 간 내부 호출 키 검사 필터]
 * - /internal/** 만 검사, 키가 설정되지 않았거나 헤더가 다르면 401 INVALID_INTERNAL_KEY
 * - 상수 시간 비교, 헤더 값은 로그에 남기지 않는다
 */
@RequiredArgsConstructor
public class InternalKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Key";
    public static final String PATH_PREFIX = "/internal/";

    private final InternalKeyProperties internalKeyProperties;
    private final ErrorResponseWriter errorResponseWriter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!matches(request.getHeader(HEADER))) {
            errorResponseWriter.write(request, response, ErrorCode.INVALID_INTERNAL_KEY);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * 키 일치 여부
     */
    private boolean matches(String given) {
        String expected = internalKeyProperties.getInboundKey();
        if (expected == null || expected.isBlank() || given == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), given.getBytes(StandardCharsets.UTF_8));
    }
}
