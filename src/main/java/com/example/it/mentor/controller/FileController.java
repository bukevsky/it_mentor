package com.example.it.mentor.controller;

import com.example.it.mentor.dto.FileUploadResponse;
import com.example.it.mentor.entity.User;
import com.example.it.mentor.entity.enums.FileType;
import com.example.it.mentor.service.FileStorage;
import com.example.it.mentor.service.StudentProfileService;
import com.example.it.mentor.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Управление загрузкой файлов")
public class FileController {

    private final FileStorage fileStorage;
    private final UserService userService;
    private final StudentProfileService studentProfileService;

    @PostMapping(value = "/resume", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadResume(@RequestParam("file") MultipartFile file) {
        User user = currentUser();
        studentProfileService.requireStudentProfile(user.getId());
        FileUploadResponse response = fileStorage.store(file, FileType.RESUME, user.getId());
        studentProfileService.linkResume(user.getId(), response.id());
        return response;
    }

    @PostMapping(value = "/portfolio", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadPortfolio(@RequestParam("file") MultipartFile file) {
        User user = currentUser();
        return fileStorage.store(file, FileType.PORTFOLIO, user.getId());
    }

    @PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = currentUser();
        FileUploadResponse response = fileStorage.store(file, FileType.AVATAR, user.getId());
        userService.linkAvatar(user.getId(), response.id());
        return response;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email);
    }
}
