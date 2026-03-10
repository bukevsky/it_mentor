package com.example.it.mentor.service;

import com.example.it.mentor.config.StorageProperties;
import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.enums.FileType;
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

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService implements FileStorage {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StoredFileRepository storedFileRepository;

    @Override
    @Transactional
    public FileUploadResponse store(MultipartFile file, FileType type, Long ownerId) {
        type.validate(file);

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

        return new FileUploadResponse(
                storedFile.getId(),
                storedFile.getOriginalFilename(),
                storedFile.getContentType(),
                storedFile.getSize(),
                storedFile.getFileType(),
                storedFile.getUploadedAt()
        );
    }

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
    }

    @Override
    public void requireOwned(Long fileId, Long ownerId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
        if (!file.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Файл принадлежит другому пользователю");
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
