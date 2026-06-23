package com.example.it.mentor.controller;

import com.example.it.mentor.dto.FileDownloadInfo;
import com.example.it.mentor.dto.FileResponse;
import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.FileType;
import com.example.it.mentor.service.FileStorage;
import com.example.it.mentor.service.StudentProfileService;
import com.example.it.mentor.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@Validated
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Управление файлами пользователей")
public class FileController {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("uploadedAt", "originalFilename", "fileType", "size");

    private final FileStorage fileStorage;
    private final UserService userService;
    private final StudentProfileService studentProfileService;

    @PostMapping(value = "/resume", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadResume(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        studentProfileService.requireStudentProfile(user.getId());
        FileUploadResponse response = fileStorage.store(file, FileType.RESUME, user.getId());
        studentProfileService.linkResume(user.getId(), response.id());
        return response;
    }

    @PostMapping(value = "/portfolio", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadPortfolio(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        return fileStorage.store(file, FileType.PORTFOLIO, user.getId());
    }

    @PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        FileUploadResponse response = fileStorage.store(file, FileType.AVATAR, user.getId());
        userService.linkAvatar(user.getId(), response.id());
        return response;
    }

    @PostMapping(value = "/chat-attachment", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadChatAttachment(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        return fileStorage.store(file, FileType.CHAT_ATTACHMENT, user.getId());
    }

    @GetMapping
    public PagedResponse<FileResponse> listFiles(
            @RequestParam(required = false) FileType type,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            @RequestParam(defaultValue = "uploadedAt,desc") String sort) {
        User user = userService.getCurrentUserEntity();
        Sort.Direction direction = sort.endsWith(",asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        String rawField = sort.contains(",") ? sort.split(",")[0] : sort;
        String field = ALLOWED_SORT_FIELDS.contains(rawField) ? rawField : "uploadedAt";
        return fileStorage.listFiles(type, user.getId(),
                PageRequest.of(page, size, Sort.by(direction, field)));
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        User user = userService.getCurrentUserEntity();
        FileDownloadInfo info = fileStorage.download(fileId, user.getId());
        Resource body = new InputStreamResource(info.stream());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(info.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + info.originalFilename() + "\"")
                .contentLength(info.size())
                .body(body);
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFile(@PathVariable Long fileId) {
        User user = userService.getCurrentUserEntity();
        fileStorage.softDeleteFile(fileId, user.getId());
    }

    @PutMapping(value = "/{fileId}/replace", consumes = MULTIPART_FORM_DATA_VALUE)
    public FileResponse replaceFile(@PathVariable Long fileId,
                                    @RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        return fileStorage.replaceFile(fileId, file, user.getId());
    }
}
