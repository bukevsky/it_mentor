package com.example.it.mentor.service;

import com.example.it.mentor.config.StorageProperties;
import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.enums.FileType;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.exception.StorageException;
import com.example.it.mentor.repository.StoredFileRepository;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

/**
 * Реализация {@link FileStorage}, сохраняющая файлы в MinIO и метаданные в БД.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService implements FileStorage {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StoredFileRepository storedFileRepository;

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46},  // %PDF
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},  // .PNG
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
    );

    private static final byte[] WEBP_RIFF   = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50}; // "WEBP"

    /**
     * Валидирует и сохраняет файл в объектное хранилище.
     *
     * @param file загружаемый файл
     * @param type тип файла с правилами валидации
     * @param ownerId идентификатор владельца файла
     * @return метаданные сохранённого файла
     */
    @Override
    @Transactional
    public FileUploadResponse store(MultipartFile file, FileType type, Long ownerId) {
        type.validate(file.getContentType(), file.getSize());
        validateFileSignature(file, type);

        String rawName = file.getOriginalFilename();
        String originalFilename = (rawName != null && !rawName.isBlank()) ? rawName : "file";
        String ext = extractExtension(originalFilename);
        String objectKey = type.name().toLowerCase() + "/" + UUID.randomUUID() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла в MinIO: objectKey={}", objectKey, e);
            throw new StorageException("Ошибка при загрузке файла: " + e.getMessage(), e);
        }

        StoredFile storedFile = StoredFile.builder()
                .originalFilename(originalFilename)
                .contentType(file.getContentType())
                .size(file.getSize())
                .storageKey(objectKey)
                .fileType(type)
                .ownerId(ownerId)
                .build();

        storedFile = storedFileRepository.save(storedFile);
        log.info("Файл загружен: fileId={}, type={}, size={}, ownerId={}", storedFile.getId(), type, file.getSize(), ownerId);

        return new FileUploadResponse(
                storedFile.getId(),
                storedFile.getOriginalFilename(),
                storedFile.getContentType(),
                storedFile.getSize(),
                storedFile.getFileType(),
                storedFile.getUploadedAt()
        );
    }

    /**
     * Удаляет файл из MinIO и из таблицы метаданных после проверки владельца.
     *
     * @param fileId идентификатор файла
     * @param requesterId идентификатор пользователя, запросившего удаление
     */
    @Override
    @Transactional
    public void delete(Long fileId, Long requesterId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));

        if (!file.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Нет доступа к файлу");
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(file.getStorageKey())
                            .build());
        } catch (Exception e) {
            log.error("Ошибка при удалении файла из MinIO: storageKey={}", file.getStorageKey(), e);
            throw new StorageException("Ошибка при удалении файла: " + e.getMessage(), e);
        }

        storedFileRepository.deleteById(fileId);
        log.info("Файл удалён: fileId={}, storageKey={}, requesterId={}", fileId, file.getStorageKey(), requesterId);
    }

    /**
     * Проверяет, что файл принадлежит ожидаемому пользователю.
     *
     * @param fileId идентификатор файла
     * @param ownerId идентификатор ожидаемого владельца
     */
    @Override
    public void requireOwned(Long fileId, Long ownerId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
        if (!file.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Файл принадлежит другому пользователю");
        }
    }

    /**
     * Проверяет сигнатуру (magic bytes) файла для предотвращения подмены Content-Type.
     */
    private void validateFileSignature(MultipartFile file, FileType type) {
        if (type.getAllowedContentTypes().isEmpty()) return;

        try {
            byte[] header = new byte[12];
            try (InputStream is = file.getInputStream()) {
                int read = is.readNBytes(header, 0, header.length);
                if (read < 3) {
                    throw new BusinessRuleViolationException("Файл слишком мал или повреждён");
                }
            }

            boolean matched = type.getAllowedContentTypes().stream()
                    .anyMatch(ct -> {
                        if ("image/webp".equals(ct)) return isWebp(header);
                        byte[] magic = MAGIC_BYTES.get(ct);
                        if (magic == null) return true; // нет проверки для этого типа
                        return Arrays.mismatch(header, 0, magic.length, magic, 0, magic.length) == -1;
                    });

            if (!matched) {
                throw new BusinessRuleViolationException("Содержимое файла не соответствует заявленному типу");
            }
        } catch (IOException e) {
            throw new StorageException("Ошибка при чтении файла: " + e.getMessage(), e);
        }
    }

    /**
     * Проверяет, что заголовок файла соответствует формату WEBP.
     *
     * @param header первые байты файла
     * @return {@code true}, если сигнатура соответствует WEBP
     */
    private static boolean isWebp(byte[] header) {
        return header.length >= 12
                && Arrays.mismatch(header, 0, 4, WEBP_RIFF, 0, 4) == -1
                && Arrays.mismatch(header, 8, 12, WEBP_MARKER, 0, 4) == -1;
    }

    /**
     * Извлекает расширение файла из исходного имени.
     *
     * @param filename исходное имя файла
     * @return расширение с точкой или пустая строка
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
