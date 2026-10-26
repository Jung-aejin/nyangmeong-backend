package com.nyangmeong.global.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nyangmeong.global.security.domain.MemberAuthority;
import com.nyangmeong.global.security.filter.InternalKeyFilter;
import com.nyangmeong.global.security.filter.JwtAuthenticationFilter;
import com.nyangmeong.global.security.filter.RequestedWithFilter;
import com.nyangmeong.global.security.handler.ErrorResponseWriter;
import com.nyangmeong.global.security.handler.RestAccessDeniedHandler;
import com.nyangmeong.global.security.handler.RestAuthenticationEntryPoint;
import com.nyangmeong.global.security.jwt.JwtTokenProvider;
import com.nyangmeong.global.security.jwt.TokenCookies;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;

/**
 * [설정 - Spring Security]
 * - STATELESS : JWT 쿠키 기반, 세션 미사용
 * - 열린 경로 : app.security.permit-all-paths (SecurityPathProperties)
 * - /api/admin/** 은 ROLE_ADMIN, 목록 밖은 전부 인증 필요
 * - /internal/** 은 X-Internal-Key 를 먼저 검사(InternalKeyProperties), 그 뒤 사용자 인증도 필요
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({SecurityPathProperties.class, InternalKeyProperties.class})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenCookies tokenCookies;
    private final AuthMemberMapper authMemberMapper;
    private final ErrorResponseWriter errorResponseWriter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final SecurityPathProperties securityPathProperties;
    private final InternalKeyProperties internalKeyProperties;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        InternalKeyFilter internalKeyFilter = new InternalKeyFilter(internalKeyProperties, errorResponseWriter);
        RequestedWithFilter requestedWithFilter = new RequestedWithFilter(errorResponseWriter);
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtTokenProvider,
                tokenCookies, authMemberMapper, errorResponseWriter);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        /** 에러 페이지 포워딩 허용 */
                        .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.ASYNC).permitAll()
                        .requestMatchers(RestAccessDeniedHandler.ADMIN_PATH_PREFIX + "**")
                        .hasAuthority(MemberAuthority.ADMIN.getRole())
                        .requestMatchers(securityPathProperties.getPermitAllPaths().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                /** 키 없는 내부 호출은 회원 조회 전에 거절 */
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestedWithFilter, InternalKeyFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestedWithFilter.class)
                .build();
    }
}
