package com.example.it.mentor.service;

import com.example.it.mentor.config.StorageProperties;
import com.example.it.mentor.dto.FileDownloadInfo;
import com.example.it.mentor.dto.FileResponse;
import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.StudentFilesResponse;
import com.example.it.mentor.entity.StoredFile;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.FileStatus;
import com.example.it.mentor.entity.enums.FileType;
import com.example.it.mentor.exception.BusinessRuleViolationException;
import com.example.it.mentor.exception.ForbiddenException;
import com.example.it.mentor.exception.NotFoundException;
import com.example.it.mentor.exception.StorageException;
import com.example.it.mentor.repository.ChatMessageRepository;
import com.example.it.mentor.repository.StoredFileRepository;
import com.example.it.mentor.repository.StudentProfileRepository;
import com.example.it.mentor.repository.UserRepository;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService implements FileStorage {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StoredFileRepository storedFileRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;

    private static final Map<String, byte[]> MAGIC_BYTES = Map.of(
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
    );

    private static final byte[] WEBP_RIFF   = {0x52, 0x49, 0x46, 0x46};
    private static final byte[] WEBP_MARKER = {0x57, 0x45, 0x42, 0x50};

    @Override
    @Transactional
    public FileUploadResponse store(MultipartFile file, FileType type, Long ownerId) {
        type.validate(file.getContentType(), file.getSize());
        validateFileSignature(file, type);

        softDeleteExistingIfSingleType(ownerId, type);

        String rawName = file.getOriginalFilename();
        String originalFilename = (rawName != null && !rawName.isBlank()) ? rawName : "file";
        String ext = extractExtension(originalFilename);
        String objectKey = type.name().toLowerCase() + "/" + UUID.randomUUID() + ext;

        long startedAt = System.nanoTime();
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
        } catch (Exception e) {
            log.error("Ошибка при загрузке файла в MinIO: objectKey={}, type={}, ownerId={}, durationMs={}, step={}",
                    objectKey, type, ownerId, durationMs(startedAt), "file_upload_failed", e);
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
        log.info("Файл загружен: fileId={}, type={}, size={}, ownerId={}, durationMs={}, step={}",
                storedFile.getId(), type, file.getSize(), ownerId, durationMs(startedAt), "file_uploaded");

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

        long startedAt = System.nanoTime();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(file.getStorageKey())
                            .build());
        } catch (Exception e) {
            log.error("Ошибка при удалении файла из MinIO: fileId={}, storageKey={}, requesterId={}, durationMs={}, step={}",
                    fileId, file.getStorageKey(), requesterId, durationMs(startedAt), "file_delete_failed", e);
            throw new StorageException("Ошибка при удалении файла: " + e.getMessage(), e);
        }

        storedFileRepository.deleteById(fileId);
        log.info("Файл удалён: fileId={}, storageKey={}, requesterId={}, durationMs={}, step={}",
                fileId, file.getStorageKey(), requesterId, durationMs(startedAt), "file_deleted");
    }

    @Override
    public void requireOwned(Long fileId, Long ownerId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));
        if (!file.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Файл принадлежит другому пользователю");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<FileResponse> listFiles(FileType type, Long ownerId, Pageable pageable) {
        Page<StoredFile> page;
        if (type != null) {
            page = storedFileRepository.findByOwnerIdAndFileTypeAndStatus(ownerId, type, FileStatus.ACTIVE, pageable);
        } else {
            page = storedFileRepository.findByOwnerIdAndStatus(ownerId, FileStatus.ACTIVE, pageable);
        }
        log.debug("Файлы пользователя загружены: ownerId={}, type={}, page={}, size={}, resultCount={}, total={}, step={}",
                ownerId, type, pageable.getPageNumber(), pageable.getPageSize(), page.getNumberOfElements(),
                page.getTotalElements(), "files_loaded");
        return PagedResponse.from(page.map(this::toFileResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownloadInfo download(Long fileId, Long requesterId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));

        if (file.getStatus() == FileStatus.DELETED) {
            throw new NotFoundException("Файл не найден");
        }

        if (!file.getOwnerId().equals(requesterId)) {
            boolean isChatParticipant = chatMessageRepository
                    .existsByAttachmentIdAndChatParticipant(fileId, requesterId);
            if (!isChatParticipant) {
                throw new ForbiddenException("Нет доступа к файлу");
            }
        }

        long startedAt = System.nanoTime();
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(file.getStorageKey())
                            .build());
            log.info("Файл подготовлен к скачиванию: fileId={}, requesterId={}, ownerId={}, size={}, durationMs={}, step={}",
                    fileId, requesterId, file.getOwnerId(), file.getSize(), durationMs(startedAt), "file_download_prepared");
            return new FileDownloadInfo(stream, file.getContentType(), file.getOriginalFilename(), file.getSize());
        } catch (Exception e) {
            log.error("Ошибка при скачивании файла из MinIO: fileId={}, requesterId={}, durationMs={}, step={}",
                    fileId, requesterId, durationMs(startedAt), "file_download_failed", e);
            throw new StorageException("Ошибка при скачивании файла: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void softDeleteFile(Long fileId, Long requesterId) {
        StoredFile file = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));

        if (!file.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Нет доступа к файлу");
        }

        if (file.getStatus() == FileStatus.DELETED) {
            return;
        }

        file.setStatus(FileStatus.DELETED);
        storedFileRepository.save(file);

        unlinkFromProfile(requesterId, file);
        log.info("Файл помечен как удалённый: fileId={}, requesterId={}, type={}, step={}",
                fileId, requesterId, file.getFileType(), "file_soft_deleted");
    }

    @Override
    @Transactional
    public FileResponse replaceFile(Long fileId, MultipartFile newFile, Long ownerId) {
        StoredFile old = storedFileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("Файл не найден"));

        if (!old.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Нет доступа к файлу");
        }

        FileType type = old.getFileType();
        type.validate(newFile.getContentType(), newFile.getSize());
        validateFileSignature(newFile, type);

        old.setStatus(FileStatus.DELETED);
        storedFileRepository.save(old);

        String rawName = newFile.getOriginalFilename();
        String originalFilename = (rawName != null && !rawName.isBlank()) ? rawName : "file";
        String ext = extractExtension(originalFilename);
        String objectKey = type.name().toLowerCase() + "/" + UUID.randomUUID() + ext;

        long startedAt = System.nanoTime();
        try (InputStream inputStream = newFile.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(storageProperties.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, newFile.getSize(), -1)
                            .contentType(newFile.getContentType())
                            .build());
        } catch (Exception e) {
            log.error("Ошибка при замене файла в MinIO: oldFileId={}, objectKey={}, ownerId={}, durationMs={}, step={}",
                    fileId, objectKey, ownerId, durationMs(startedAt), "file_replace_failed", e);
            throw new StorageException("Ошибка при замене файла: " + e.getMessage(), e);
        }

        StoredFile newStored = StoredFile.builder()
                .originalFilename(originalFilename)
                .contentType(newFile.getContentType())
                .size(newFile.getSize())
                .storageKey(objectKey)
                .fileType(type)
                .ownerId(ownerId)
                .build();
        newStored = storedFileRepository.save(newStored);

        relinkToProfile(ownerId, type, newStored.getId());

        log.info("Файл заменён: oldFileId={}, newFileId={}, ownerId={}, type={}, size={}, durationMs={}, step={}",
                fileId, newStored.getId(), ownerId, type, newFile.getSize(), durationMs(startedAt), "file_replaced");
        return toFileResponse(newStored);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentFilesResponse getStudentFiles(Long userId) {
        FileResponse resume = storedFileRepository
                .findFirstByOwnerIdAndFileTypeAndStatus(userId, FileType.RESUME, FileStatus.ACTIVE)
                .map(this::toFileResponse)
                .orElse(null);

        int portfolioCount = (int) storedFileRepository
                .countByOwnerIdAndFileTypeAndStatus(userId, FileType.PORTFOLIO, FileStatus.ACTIVE);

        Long avatarFileId = userRepository.findById(userId)
                .map(User::getAvatarFileId)
                .orElse(null);

        StudentFilesResponse response = new StudentFilesResponse(resume, portfolioCount, avatarFileId);
        log.debug("Файлы студента загружены: userId={}, hasResume={}, portfolioCount={}, hasAvatar={}, step={}",
                userId, resume != null, portfolioCount, avatarFileId != null, "student_files_loaded");
        return response;
    }

    private void softDeleteExistingIfSingleType(Long ownerId, FileType type) {
        if (type != FileType.RESUME && type != FileType.AVATAR) {
            return;
        }
        List<StoredFile> existing = storedFileRepository
                .findAllByOwnerIdAndFileTypeAndStatus(ownerId, type, FileStatus.ACTIVE);
        for (StoredFile f : existing) {
            f.setStatus(FileStatus.DELETED);
            storedFileRepository.save(f);
            log.debug("Старый файл одиночного типа помечен как удалённый: fileId={}, ownerId={}, type={}, step={}",
                    f.getId(), ownerId, type, "single_type_old_file_soft_deleted");
        }
    }

    private void unlinkFromProfile(Long userId, StoredFile file) {
        if (file.getFileType() == FileType.RESUME) {
            studentProfileRepository.findByUserId(userId).ifPresent(profile -> {
                if (file.getId().equals(profile.getResumeFileId())) {
                    profile.setResumeFileId(null);
                    studentProfileRepository.save(profile);
                }
            });
        } else if (file.getFileType() == FileType.AVATAR) {
            userRepository.findById(userId).ifPresent(user -> {
                if (file.getId().equals(user.getAvatarFileId())) {
                    user.setAvatarFileId(null);
                    userRepository.save(user);
                }
            });
        }
    }

    private void relinkToProfile(Long userId, FileType type, Long newFileId) {
        if (type == FileType.RESUME) {
            studentProfileRepository.findByUserId(userId).ifPresent(profile -> {
                profile.setResumeFileId(newFileId);
                studentProfileRepository.save(profile);
            });
        } else if (type == FileType.AVATAR) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setAvatarFileId(newFileId);
                userRepository.save(user);
            });
        }
    }

    private FileResponse toFileResponse(StoredFile file) {
        String previewUrl = file.getContentType() != null && file.getContentType().startsWith("image/")
                ? "/files/" + file.getId() + "/download"
                : null;
        return new FileResponse(
                file.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getFileType().name(),
                file.getStatus().name(),
                previewUrl,
                file.getUploadedAt()
        );
    }

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
                        if (magic == null) return true;
                        return Arrays.mismatch(header, 0, magic.length, magic, 0, magic.length) == -1;
                    });

            if (!matched) {
                throw new BusinessRuleViolationException("Содержимое файла не соответствует заявленному типу");
            }
        } catch (IOException e) {
            throw new StorageException("Ошибка при чтении файла: " + e.getMessage(), e);
        }
    }

    private static boolean isWebp(byte[] header) {
        return header.length >= 12
                && Arrays.mismatch(header, 0, 4, WEBP_RIFF, 0, 4) == -1
                && Arrays.mismatch(header, 8, 12, WEBP_MARKER, 0, 4) == -1;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }

    private static long durationMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
