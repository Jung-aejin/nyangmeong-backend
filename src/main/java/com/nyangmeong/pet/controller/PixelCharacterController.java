package com.nyangmeong.pet.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nyangmeong.global.response.ApiResponse;
import com.nyangmeong.global.security.domain.LoginMember;
import com.nyangmeong.pet.dto.EquippedItemSaveRequest;
import com.nyangmeong.pet.dto.PixelCharacterResponse;
import com.nyangmeong.pet.dto.PixelCharacterSummary;
import com.nyangmeong.pet.service.PixelCharacterService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * [픽셀 캐릭터 컨트롤러]
 * - SCR-MYP-010 목록·꾸미기
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pixel-characters")
public class PixelCharacterController {

    private final PixelCharacterService pixelCharacterService;

    @Operation(summary = "내 캐릭터·공동양육 캐릭터 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PixelCharacterSummary>>> search(@AuthenticationPrincipal LoginMember me) {
        return ResponseEntity.ok(ApiResponse.success(pixelCharacterService.search(me.id())));
    }

    @Operation(summary = "캐릭터 꾸미기 상세")
    @GetMapping("/{pixelCharSn}")
    public ResponseEntity<ApiResponse<PixelCharacterResponse>> getPixelCharacter(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn) {
        return ResponseEntity.ok(ApiResponse.success(pixelCharacterService.getPixelCharacter(me.id(), pixelCharSn)));
    }

    @Operation(summary = "착용 상태 저장")
    @PutMapping("/{pixelCharSn}/equipped-items")
    public ResponseEntity<ApiResponse<PixelCharacterResponse>> saveEquippedItems(@AuthenticationPrincipal LoginMember me,
            @PathVariable Long pixelCharSn, @Valid @RequestBody EquippedItemSaveRequest req) {
        return ResponseEntity.ok(ApiResponse.success(pixelCharacterService.saveEquippedItems(me.id(), pixelCharSn, req)));
    }
}
