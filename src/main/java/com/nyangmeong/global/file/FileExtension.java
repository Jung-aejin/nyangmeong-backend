package com.nyangmeong.global.file;

import java.util.Arrays;
import java.util.Optional;

/**
 * [파일 확장자 코드]
 * - FILG01 (ATCH_FILE.FILE_EXTN_CD)
 */
public enum FileExtension {
    JPG("FILC0001", "jpg", "image/jpeg"),
    JPEG("FILC0002", "jpeg", "image/jpeg"),
    PNG("FILC0003", "png", "image/png"),
    WEBP("FILC0004", "webp", "image/webp"),
    PDF("FILC0005", "pdf", "application/pdf");

    private final String code;
    private final String extension;
    private final String contentType;

    FileExtension(String code, String extension, String contentType) {
        this.code = code;
        this.extension = extension;
        this.contentType = contentType;
    }

    public String getCode() {
        return code;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /**
     * 이미지 확장자 여부
     */
    public boolean isImage() {
        return this != PDF;
    }

    /**
     * 코드로 확장자 조회
     */
    public static FileExtension from(String code) {
        return Arrays.stream(values())
                .filter(extension -> extension.is(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 파일 확장자 코드: " + code));
    }

    /**
     * 파일 이름의 확장자로 조회
     */
    public static Optional<FileExtension> ofFileName(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') < 0) {
            return Optional.empty();
        }
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return Arrays.stream(values())
                .filter(value -> value.extension.equals(extension))
                .findFirst();
    }
}
