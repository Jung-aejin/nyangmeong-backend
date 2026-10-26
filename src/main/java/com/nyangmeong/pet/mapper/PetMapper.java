package com.nyangmeong.pet.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.nyangmeong.pet.dto.PetSummary;

/**
 * [반려동물 Mapper]
 * - PET·BRUP_REL 조회 (반려동물 관리)
 */
@Mapper
public interface PetMapper {

    /**
     * 회원의 양육 반려동물 목록 조회
     */
    List<PetSummary> selectList(@Param("mbrSn") Long mbrSn, @Param("coParentRoleCd") String coParentRoleCd);
}
