package com.nyangmeong.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.auth.dto.AuthMeResponse;
import com.nyangmeong.auth.service.AuthService;
import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [인증 주체 조회 컨트롤러]
 * - GET /api/auth/me
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "내 정보(헤더 프로필·드롭다운)")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> getMe(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(me.id())));
    }
}
