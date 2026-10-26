package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.service.PetService;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;

/**
 * [반려동물 컨트롤러]
 * - 내 반려동물 목록 (SCR-MYP-001 카드, SCR-MYP-002 슬라이드)
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pets")
public class PetController {

    private final PetService petService;

    @Operation(summary = "내 반려동물 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetSummary>>> searchMyPets(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(petService.searchMyPets(me.id())));
    }
}
