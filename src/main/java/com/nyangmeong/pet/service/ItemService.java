package com.nyangmeong.pet.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.file.StoredFile;
import com.nyangmeong.pet.mapper.ItemMapper;

import lombok.RequiredArgsConstructor;

/**
 * [아이템 서비스]
 * - 아이템 이미지 = 상점 공용 자원, 반려동물 권한 검사 없음
 */
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemMapper itemMapper;
    private final FileStore fileStore;

    /**
     * 아이템 이미지 조회
     */
    @Transactional(readOnly = true)
    public StoredFile getImage(Long itemSn) {
        Long atchFileSn = itemMapper.selectAtchFileSn(itemSn);
        if (atchFileSn == null) {
            throw new CustomException(ErrorCode.ITEM_NOT_FOUND);
        }
        return fileStore.load(atchFileSn);
    }
}
