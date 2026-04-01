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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService")
class FileStorageServiceTest {

    @Mock private MinioClient minioClient;
    @Mock private StorageProperties storageProperties;
    @Mock private StoredFileRepository storedFileRepository;

    @InjectMocks
    private FileStorageService fileStorageService;

    // ── store() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("store()")
    class Store {

        // ── RESUME ────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("RESUME")
        class Resume {

            @Test
            @DisplayName("PDF файл → сохраняется в Minio и БД, возвращает корректный response")
            void validPdf_shouldSaveAndReturnResponse() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "resume.pdf", "application/pdf", pdfContent(1024));
                StoredFile saved = buildStoredFile(1L, "resume.pdf", "application/pdf",
                        1024L, "resume/uuid.pdf", FileType.RESUME, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                FileUploadResponse response = fileStorageService.store(file, FileType.RESUME, 1L);

                assertThat(response.id()).as("id").isEqualTo(1L);
                assertThat(response.fileType()).as("fileType").isEqualTo(FileType.RESUME);
                verify(minioClient).putObject(any(PutObjectArgs.class));
                verify(storedFileRepository).save(any());
            }

            @Test
            @DisplayName("PDF ровно на границе лимита (5MB) → успешно сохраняется")
            void pdfExactlyAtSizeLimit_shouldSave() throws Exception {
                int exactLimit = 5 * 1024 * 1024; // ровно 5 МБ
                MockMultipartFile file = new MockMultipartFile(
                        "file", "resume.pdf", "application/pdf", pdfContent(exactLimit));
                StoredFile saved = buildStoredFile(1L, "resume.pdf", "application/pdf",
                        (long) exactLimit, "resume/uuid.pdf", FileType.RESUME, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                assertThatNoException().isThrownBy(() ->
                        fileStorageService.store(file, FileType.RESUME, 1L));
            }

            @Test
            @DisplayName("JPEG вместо PDF → BusinessRuleViolationException с сообщением о формате")
            void jpeg_shouldThrowWithContentTypeMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "resume.jpg", "image/jpeg", new byte[1024]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.RESUME, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("PDF");

                verify(storedFileRepository, never()).save(any());
            }

            @Test
            @DisplayName("файл больше 5MB → BusinessRuleViolationException с сообщением о размере")
            void oversizedPdf_shouldThrowWithSizeMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "resume.pdf", "application/pdf", new byte[5 * 1024 * 1024 + 1]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.RESUME, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("5 МБ");

                verify(storedFileRepository, never()).save(any());
            }

            @ParameterizedTest
            @ValueSource(strings = {"image/jpeg", "image/png", "image/webp", "application/msword"})
            @DisplayName("недопустимые типы для резюме → BusinessRuleViolationException")
            void disallowedContentTypes_shouldThrow(String contentType) {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "resume.bin", contentType, new byte[1024]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.RESUME, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class);
            }

            @Test
            @DisplayName("getOriginalFilename() возвращает null → originalFilename в сохранённой entity равен \"file\", исключений нет")
            void nullOriginalFilename_shouldFallbackToFile() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", null, "application/pdf", pdfContent(1024));
                StoredFile saved = buildStoredFile(1L, "file", "application/pdf",
                        1024L, "resume/uuid", FileType.RESUME, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                FileUploadResponse response = fileStorageService.store(file, FileType.RESUME, 1L);

                ArgumentCaptor<StoredFile> captor = ArgumentCaptor.forClass(StoredFile.class);
                verify(storedFileRepository).save(captor.capture());
                assertThat(captor.getValue().getOriginalFilename()).isEqualTo("file");
                assertThat(response.originalFilename()).isEqualTo("file");
            }
        }

        // ── PORTFOLIO ─────────────────────────────────────────────────────────

        @Nested
        @DisplayName("PORTFOLIO")
        class Portfolio {

            @ParameterizedTest
            @CsvSource({
                    "portfolio.jpg, image/jpeg",
                    "portfolio.png, image/png",
                    "portfolio.pdf, application/pdf"
            })
            @DisplayName("допустимые типы (JPEG, PNG, PDF) → успешное сохранение")
            void allowedTypes_shouldSave(String filename, String contentType) throws Exception {
                MockMultipartFile file = new MockMultipartFile("file", filename, contentType, contentForType(contentType, 1024));
                StoredFile saved = buildStoredFile(2L, filename, contentType,
                        1024L, "portfolio/uuid", FileType.PORTFOLIO, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                FileUploadResponse response = fileStorageService.store(file, FileType.PORTFOLIO, 1L);

                assertThat(response.fileType()).isEqualTo(FileType.PORTFOLIO);
            }

            @Test
            @DisplayName("DOCX → BusinessRuleViolationException с сообщением о допустимых форматах")
            void docx_shouldThrowWithFormatMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "doc.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        new byte[1024]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.PORTFOLIO, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("PDF");
            }

            @Test
            @DisplayName("файл больше 10MB → BusinessRuleViolationException с сообщением о размере")
            void oversized_shouldThrowWithSizeMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "portfolio.pdf", "application/pdf", new byte[10 * 1024 * 1024 + 1]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.PORTFOLIO, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("10 МБ");
            }

            @Test
            @DisplayName("ровно 10MB → успешно сохраняется")
            void exactlyAtLimit_shouldSave() throws Exception {
                int limit = 10 * 1024 * 1024;
                MockMultipartFile file = new MockMultipartFile(
                        "file", "portfolio.pdf", "application/pdf", pdfContent(limit));
                StoredFile saved = buildStoredFile(2L, "portfolio.pdf", "application/pdf",
                        (long) limit, "portfolio/uuid.pdf", FileType.PORTFOLIO, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                assertThatNoException().isThrownBy(() ->
                        fileStorageService.store(file, FileType.PORTFOLIO, 1L));
            }
        }

        // ── AVATAR ────────────────────────────────────────────────────────────

        @Nested
        @DisplayName("AVATAR")
        class Avatar {

            @ParameterizedTest
            @CsvSource({
                    "avatar.jpg, image/jpeg",
                    "avatar.png, image/png",
                    "avatar.webp, image/webp"
            })
            @DisplayName("допустимые типы (JPEG, PNG, WebP) → успешное сохранение")
            void allowedTypes_shouldSave(String filename, String contentType) throws Exception {
                MockMultipartFile file = new MockMultipartFile("file", filename, contentType, contentForType(contentType, 1024));
                StoredFile saved = buildStoredFile(3L, filename, contentType,
                        1024L, "avatar/uuid", FileType.AVATAR, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                FileUploadResponse response = fileStorageService.store(file, FileType.AVATAR, 1L);

                assertThat(response.fileType()).isEqualTo(FileType.AVATAR);
            }

            @Test
            @DisplayName("WAV-файл (RIFF, не WebP) с типом image/webp → BusinessRuleViolationException")
            void wavDisguisedAsWebp_shouldThrow() {
                // WAV: RIFF at 0-3, but bytes 8-11 are NOT "WEBP"
                byte[] wavContent = new byte[1024];
                System.arraycopy(WEBP_RIFF, 0, wavContent, 0, 4);
                MockMultipartFile file = new MockMultipartFile(
                        "file", "audio.wav", "image/webp", wavContent);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.AVATAR, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class);
            }

            @Test
            @DisplayName("PDF вместо изображения → BusinessRuleViolationException с сообщением о форматах")
            void pdf_shouldThrowWithFormatMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "avatar.pdf", "application/pdf", new byte[1024]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.AVATAR, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("JPEG");
            }

            @Test
            @DisplayName("файл больше 2MB → BusinessRuleViolationException с сообщением о размере")
            void oversized_shouldThrowWithSizeMessage() {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "avatar.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1]);

                assertThatThrownBy(() -> fileStorageService.store(file, FileType.AVATAR, 1L))
                        .isInstanceOf(BusinessRuleViolationException.class)
                        .hasMessageContaining("2 МБ");
            }

            @Test
            @DisplayName("ровно 2MB → успешно сохраняется")
            void exactlyAtLimit_shouldSave() throws Exception {
                int limit = 2 * 1024 * 1024;
                MockMultipartFile file = new MockMultipartFile(
                        "file", "avatar.jpg", "image/jpeg", jpegContent(limit));
                StoredFile saved = buildStoredFile(3L, "avatar.jpg", "image/jpeg",
                        (long) limit, "avatar/uuid.jpg", FileType.AVATAR, 1L);

                when(storageProperties.getBucketName()).thenReturn("itmentor-test");
                doReturn(null).when(minioClient).putObject(any(PutObjectArgs.class));
                when(storedFileRepository.save(any())).thenReturn(saved);

                assertThatNoException().isThrownBy(() ->
                        fileStorageService.store(file, FileType.AVATAR, 1L));
            }
        }

        @Test
        @DisplayName("MinIO выбрасывает исключение → StorageException с причиной")
        void store_minioThrowsException_shouldThrowStorageException() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "resume.pdf", "application/pdf", pdfContent(1024));

            when(storageProperties.getBucketName()).thenReturn("itmentor-test");
            doThrow(new RuntimeException("MinIO connection refused"))
                    .when(minioClient).putObject(any(PutObjectArgs.class));

            assertThatThrownBy(() -> fileStorageService.store(file, FileType.RESUME, 1L))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("Ошибка при загрузке файла")
                    .hasCauseInstanceOf(RuntimeException.class);

            verify(storedFileRepository, never()).save(any());
        }
    }

    // ── delete() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("владелец удаляет свой файл → удаляется из Minio и из БД")
        void owner_shouldDeleteFromMinioAndDb() throws Exception {
            StoredFile file = buildStoredFile(1L, null, null, null,
                    "resume/uuid.pdf", FileType.RESUME, 1L);
            when(storedFileRepository.findById(1L)).thenReturn(Optional.of(file));
            when(storageProperties.getBucketName()).thenReturn("itmentor-test");
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

            fileStorageService.delete(1L, 1L);

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
            verify(storedFileRepository).deleteById(1L);
        }

        @Test
        @DisplayName("не владелец пытается удалить → ForbiddenException, файл не удаляется")
        void notOwner_shouldThrowForbiddenAndNotDelete() {
            StoredFile file = buildStoredFile(1L, null, null, null,
                    "resume/uuid.pdf", FileType.RESUME, 1L);
            when(storedFileRepository.findById(1L)).thenReturn(Optional.of(file));

            assertThatThrownBy(() -> fileStorageService.delete(1L, 2L))
                    .isInstanceOf(ForbiddenException.class);

            verify(storedFileRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("файл не найден → NotFoundException")
        void fileNotFound_shouldThrowNotFoundException() {
            when(storedFileRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fileStorageService.delete(99L, 1L))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("MinIO выбрасывает исключение при удалении → StorageException")
        void delete_minioThrowsException_shouldThrowStorageException() throws Exception {
            StoredFile file = buildStoredFile(1L, null, null, null,
                    "resume/uuid.pdf", FileType.RESUME, 1L);
            when(storedFileRepository.findById(1L)).thenReturn(Optional.of(file));
            when(storageProperties.getBucketName()).thenReturn("itmentor-test");
            doThrow(new RuntimeException("MinIO connection refused"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            assertThatThrownBy(() -> fileStorageService.delete(1L, 1L))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("Ошибка при удалении файла")
                    .hasCauseInstanceOf(RuntimeException.class);

            verify(storedFileRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("владелец удаляет файл другого типа (AVATAR) → успешно")
        void ownerDeletesAvatar_shouldSucceed() throws Exception {
            StoredFile file = buildStoredFile(5L, null, null, null,
                    "avatar/uuid.jpg", FileType.AVATAR, 42L);
            when(storedFileRepository.findById(5L)).thenReturn(Optional.of(file));
            when(storageProperties.getBucketName()).thenReturn("itmentor-test");
            doNothing().when(minioClient).removeObject(any(RemoveObjectArgs.class));

            fileStorageService.delete(5L, 42L);

            verify(storedFileRepository).deleteById(5L);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static final byte[] PDF_MAGIC    = {0x25, 0x50, 0x44, 0x46};           // %PDF
    private static final byte[] JPEG_MAGIC   = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC    = {(byte) 0x89, 0x50, 0x4E, 0x47};   // .PNG
    private static final byte[] WEBP_RIFF    = {0x52, 0x49, 0x46, 0x46};           // RIFF
    private static final byte[] WEBP_MARKER  = {0x57, 0x45, 0x42, 0x50};           // WEBP

    private static byte[] fileContent(byte[] magic, int totalSize) {
        byte[] content = new byte[Math.max(totalSize, magic.length + 4)];
        System.arraycopy(magic, 0, content, 0, magic.length);
        return content;
    }

    private static byte[] pdfContent(int size)  { return fileContent(PDF_MAGIC, size); }
    private static byte[] jpegContent(int size) { return fileContent(JPEG_MAGIC, size); }
    private static byte[] pngContent(int size)  { return fileContent(PNG_MAGIC, size); }

    /** Proper WEBP: bytes 0-3 = "RIFF", bytes 8-11 = "WEBP". */
    private static byte[] webpContent(int size) {
        byte[] content = new byte[Math.max(size, 12)];
        System.arraycopy(WEBP_RIFF,   0, content, 0, 4);
        System.arraycopy(WEBP_MARKER, 0, content, 8, 4);
        return content;
    }

    private static byte[] contentForType(String contentType, int size) {
        return switch (contentType) {
            case "application/pdf" -> pdfContent(size);
            case "image/jpeg" -> jpegContent(size);
            case "image/png" -> pngContent(size);
            case "image/webp" -> webpContent(size);
            default -> new byte[size];
        };
    }

    private StoredFile buildStoredFile(Long id, String originalFilename, String contentType,
                                       Long size, String storageKey, FileType fileType, Long ownerId) {
        StoredFile file = StoredFile.builder()
                .originalFilename(originalFilename)
                .contentType(contentType)
                .size(size)
                .storageKey(storageKey)
                .fileType(fileType)
                .ownerId(ownerId)
                .uploadedAt(OffsetDateTime.now())
                .build();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }
}
