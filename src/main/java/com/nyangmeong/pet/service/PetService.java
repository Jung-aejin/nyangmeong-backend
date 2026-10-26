package com.nyangmeong.pet.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.auth.BrupRole;
import com.nyangmeong.pet.dto.PetSummary;
import com.nyangmeong.pet.mapper.PetMapper;

import lombok.RequiredArgsConstructor;

/**
 * [반려동물 서비스]
 * - 내 반려동물 목록 (SCR-MYP-001·002), 다른 도메인도 이 목록을 씀
 * - 정렬 = 내 양육관계 등록순 (docs/api/issues.md 마이페이지 1번 결정 전까지)
 */
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetMapper petMapper;

    /**
     * 내 반려동물 목록 조회
     */
    @Transactional(readOnly = true)
    public List<PetSummary> searchMyPets(Long mbrSn) {
        return petMapper.selectList(mbrSn, BrupRole.CO_PARENT.getCode());
    }
}
