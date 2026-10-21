package com.nyangmeong.global.file;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.setting.SystemSettingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * [파일 저장·읽기]
 * - 디스크(app.file.root-dir) + ATCH_FILE 행, 확장자 jpg·jpeg·png·webp, 크기 상한 SYS_STNG.FILE_MAX_SZ
 * - 트랜잭션 롤백 시 디스크 파일도 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStore {

    private static final int ORGNL_FILE_NM_MAX = 200;
    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM");

    private final AtchFileMapper atchFileMapper;
    private final FileProperties fileProperties;
    private final SystemSettingService systemSettingService;

    /**
     * 업로드 이미지 저장
     */
    public Long store(MultipartFile file, String actorId) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        FileExtension extension = FileExtension.ofFileName(file.getOriginalFilename())
                .filter(FileExtension::isImage)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_TYPE_NOT_ALLOWED));
        if (file.getSize() > systemSettingService.get().getFileMaxSz()) {
            throw new CustomException(ErrorCode.FILE_TOO_LARGE);
        }
        try {
            return save(file.getBytes(), extension, file.getOriginalFilename(), actorId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 서버 생성 파일 저장
     */
    public Long store(byte[] content, FileExtension extension, String orgnlFileNm, String actorId) {
        return save(content, extension, orgnlFileNm, actorId);
    }

    /**
     * 파일 읽기
     */
    public StoredFile load(Long atchFileSn) {
        AtchFile atchFile = atchFileMapper.selectInUse(atchFileSn);
        if (atchFile == null) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        Path path = resolve(atchFile.getPhysStrgPath());
        if (!Files.isRegularFile(path)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        try {
            return new StoredFile(Files.readAllBytes(path),
                    FileExtension.from(atchFile.getFileExtnCd()).getContentType(), atchFile.getOrgnlFileNm());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 디스크 저장과 ATCH_FILE 등록
     */
    private Long save(byte[] content, FileExtension extension, String orgnlFileNm, String actorId) {
        String physStrgPath = LocalDate.now().format(DIR_FORMAT) + "/" + UUID.randomUUID() + "." + extension.getExtension();
        Path path = resolve(physStrgPath);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        deleteOnRollback(path);

        AtchFile atchFile = new AtchFile();
        atchFile.setFileExtnCd(extension.getCode());
        atchFile.setOrgnlFileNm(truncate(orgnlFileNm));
        atchFile.setPhysStrgPath(physStrgPath);
        atchFile.setFileSz((long) content.length);
        atchFile.setRgtrId(actorId);
        atchFileMapper.insert(atchFile);
        return atchFile.getAtchFileSn();
    }

    /**
     * 저장 경로 계산
     * - 루트 밖 경로 차단
     */
    private Path resolve(String physStrgPath) {
        Path root = Path.of(fileProperties.getRootDir()).toAbsolutePath().normalize();
        Path path = root.resolve(physStrgPath).normalize();
        if (!path.startsWith(root)) {
            throw new CustomException(ErrorCode.FILE_NOT_FOUND);
        }
        return path;
    }

    /**
     * 롤백 시 파일 삭제 등록
     */
    private void deleteOnRollback(Path path) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        log.warn("[FileStore] 롤백 파일 삭제 실패 path={}", path, e);
                    }
                }
            }
        });
    }

    /**
     * 원본 파일명 길이 제한
     */
    private String truncate(String orgnlFileNm) {
        String name = orgnlFileNm == null || orgnlFileNm.isBlank() ? "file" : orgnlFileNm;
        return name.length() > ORGNL_FILE_NM_MAX ? name.substring(name.length() - ORGNL_FILE_NM_MAX) : name;
    }
}
