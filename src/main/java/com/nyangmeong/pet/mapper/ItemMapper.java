package com.nyangmeong.pet.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 * [아이템 Mapper]
 */
@Mapper
public interface ItemMapper {

    /**
     * 아이템 이미지 첨부파일 번호 조회
     */
    Long selectAtchFileSn(Long itemSn);
}
