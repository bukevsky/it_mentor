package com.example.it.mentor.controller;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.dto.LoginRequest;
import com.example.it.mentor.dto.LoginResponse;
import com.example.it.mentor.dto.RegisterRequest;
import com.example.it.mentor.entity.enums.FileType;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestRestTemplate
@DisplayName("FileController IT")
class FileControllerIT {

    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest");

    @DynamicPropertySource
    static void minioProps(DynamicPropertyRegistry registry) {
        registry.add("app.storage.endpoint", minioContainer::getS3URL);
        registry.add("app.storage.access-key", minioContainer::getUserName);
        registry.add("app.storage.secret-key", minioContainer::getPassword);
        registry.add("app.storage.bucket-name", () -> "itmentor-test");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    // ── POST /files/resume ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /files/resume")
    class UploadResume {

        @Test
        @DisplayName("PDF → 201, id и fileType=RESUME в ответе")
        void validPdf_shouldReturn201WithResponse() {
            String token = registerAndLogin();

            ResponseEntity<FileUploadResponse> response = uploadFile(
                    "/files/resume", "resume.pdf", "application/pdf",
                    validContent("application/pdf"), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();

            SoftAssertions softly = new SoftAssertions();
            softly.assertThat(response.getBody().id()).as("id").isNotNull();
            softly.assertThat(response.getBody().fileType()).as("fileType").isEqualTo(FileType.RESUME);
            softly.assertAll();
        }

        @Test
        @DisplayName("JPEG вместо PDF → 422 Unprocessable Content")
        void nonPdf_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/resume", "resume.jpg", "image/jpeg", new byte[1024], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("PNG вместо PDF → 422")
        void png_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/resume", "resume.png", "image/png", new byte[1024], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("PDF больше 5MB → 422")
        void oversizedResume_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/resume", "big.pdf", "application/pdf",
                    new byte[5 * 1024 * 1024 + 1], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = uploadFileNoAuth(
                    "/files/resume", "resume.pdf", "application/pdf", new byte[1024]);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── POST /files/portfolio ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /files/portfolio")
    class UploadPortfolio {

        @ParameterizedTest
        @CsvSource({
                "portfolio.jpg, image/jpeg",
                "portfolio.png, image/png",
                "portfolio.pdf, application/pdf"
        })
        @DisplayName("допустимые типы (JPEG, PNG, PDF) → 201 и fileType=PORTFOLIO")
        void allowedTypes_shouldReturn201(String filename, String contentType) {
            String token = registerAndLogin();

            ResponseEntity<FileUploadResponse> response = uploadFile(
                    "/files/portfolio", filename, contentType, validContent(contentType), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody().fileType()).isEqualTo(FileType.PORTFOLIO);
        }

        @Test
        @DisplayName("DOCX → 422")
        void docx_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/portfolio", "doc.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    new byte[1024], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        // Примечание: тест на файл > 10MB не добавляется, так как Spring multipart
        // max-request-size = 10MB и сервер закроет соединение раньше, чем FileType.validate()
        // вернёт 422. Граничное условие покрыто на уровне unit-тестов FileStorageServiceTest.

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = uploadFileNoAuth(
                    "/files/portfolio", "portfolio.pdf", "application/pdf", new byte[1024]);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ── POST /files/avatar ────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /files/avatar")
    class UploadAvatar {

        @ParameterizedTest
        @CsvSource({
                "avatar.jpg, image/jpeg",
                "avatar.png, image/png",
                "avatar.webp, image/webp"
        })
        @DisplayName("допустимые типы (JPEG, PNG, WebP) → 201 и fileType=AVATAR")
        void allowedTypes_shouldReturn201WithAvatarType(String filename, String contentType) {
            String token = registerAndLogin();

            ResponseEntity<FileUploadResponse> response = uploadFile(
                    "/files/avatar", filename, contentType, validContent(contentType), token);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().fileType()).isEqualTo(FileType.AVATAR);
        }

        @Test
        @DisplayName("PDF вместо изображения → 422")
        void pdf_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/avatar", "avatar.pdf", "application/pdf",
                    new byte[1024], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("аватар больше 2MB → 422")
        void oversizedAvatar_shouldReturn422() {
            String token = registerAndLogin();

            ResponseEntity<Object> response = uploadFile(
                    "/files/avatar", "avatar.jpg", "image/jpeg",
                    new byte[2 * 1024 * 1024 + 1], token, Object.class);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        @DisplayName("без токена → 401")
        void withoutToken_shouldReturn401() {
            ResponseEntity<Object> response = uploadFileNoAuth(
                    "/files/avatar", "avatar.jpg", "image/jpeg", new byte[1024]);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("повторная загрузка аватара → 201 (пользователь может заменить аватар)")
        void uploadTwice_shouldReturn201BothTimes() {
            String token = registerAndLogin();

            ResponseEntity<FileUploadResponse> first = uploadFile(
                    "/files/avatar", "avatar1.jpg", "image/jpeg", validContent("image/jpeg"), token);
            ResponseEntity<FileUploadResponse> second = uploadFile(
                    "/files/avatar", "avatar2.png", "image/png", validContent("image/png"), token);

            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(second.getBody().id())
                    .as("Второй аватар должен получить новый id")
                    .isNotEqualTo(first.getBody().id());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String registerAndLogin() {
        String email = "file_" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
        restTemplate.postForEntity("/auth/register",
                new RegisterRequest(email, "Password123", "Иван", "Иванов"), Object.class);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/auth/login", new LoginRequest(email, "Password123"), LoginResponse.class);
        return loginResponse.getBody().accessToken();
    }

    /**
     * Создаёт байтовый массив размером 1024 байта с корректными magic bytes для указанного MIME-типа.
     * Используется в тестах, где файл должен пройти валидацию сигнатуры.
     */
    private static byte[] validContent(String contentType) {
        if ("image/webp".equals(contentType)) {
            // WebP: "RIFF" at 0-3, 4 bytes file size, "WEBP" at 8-11
            byte[] content = new byte[1024];
            byte[] riff = {0x52, 0x49, 0x46, 0x46};
            byte[] webp = {0x57, 0x45, 0x42, 0x50};
            System.arraycopy(riff, 0, content, 0, 4);
            System.arraycopy(webp, 0, content, 8, 4);
            return content;
        }
        byte[] magic = switch (contentType) {
            case "application/pdf" -> new byte[]{0x25, 0x50, 0x44, 0x46}; // %PDF
            case "image/png" -> new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}; // .PNG
            case "image/jpeg" -> new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
            default -> new byte[0];
        };
        byte[] content = new byte[1024];
        System.arraycopy(magic, 0, content, 0, magic.length);
        return content;
    }

    private ResponseEntity<FileUploadResponse> uploadFile(
            String url, String filename, String contentType, byte[] content, String token) {
        return uploadFile(url, filename, contentType, content, token, FileUploadResponse.class);
    }

    private <T> ResponseEntity<T> uploadFile(
            String url, String filename, String contentType, byte[] content,
            String token, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(token);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        partHeaders.setContentDispositionFormData("file", filename);
        body.add("file", new HttpEntity<>(new ByteArrayResource(content), partHeaders));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, responseType);
    }

    private ResponseEntity<Object> uploadFileNoAuth(
            String url, String filename, String contentType, byte[] content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        partHeaders.setContentDispositionFormData("file", filename);
        body.add("file", new HttpEntity<>(new ByteArrayResource(content), partHeaders));

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(url, request, Object.class);
    }
}
