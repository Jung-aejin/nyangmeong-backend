package com.nyangmeong.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 로그인(Security)을 붙이기 전까지 쓰는 개발용 설정. 모든 요청을 허용한다(docs/OPEN_ISSUES.md).
 * prod 프로필에서는 로드되지 않아 Spring Boot 기본 보안(전부 막힘)이 그대로 켜진다.
 * 로그인을 구현할 때 이 클래스를 지우고 SecurityConfig로 바꾼다.
 */
@Configuration
@Profile("!prod")
public class DevSecurityConfig {

    @Bean
    SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
