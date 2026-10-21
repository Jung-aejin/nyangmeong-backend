package com.nyangmeong.global.file;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * [파일 저장 설정]
 * - app.file.root-dir, 키가 없는 PC 대비 실행 폴더 아래 uploads
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    /** 저장 루트 폴더 */
    private String rootDir = "uploads";
}
