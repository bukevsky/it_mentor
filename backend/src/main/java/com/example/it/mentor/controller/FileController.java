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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

/**
 * REST-контроллер для загрузки файлов пользователей.
 *
 * <p>Все эндпоинты требуют аутентификации. Файлы сохраняются в MinIO через
 * {@link com.example.it.mentor.service.FileStorage}. Валидация типа и размера
 * файла делегируется {@link com.example.it.mentor.entity.enums.FileType#validate}.</p>
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Управление загрузкой файлов")
public class FileController {

    private final FileStorage fileStorage;
    private final UserService userService;
    private final StudentProfileService studentProfileService;

    /**
     * Загружает резюме студента.
     *
     * <p>Требует наличия профиля студента — при его отсутствии выбрасывается
     * {@link com.example.it.mentor.exception.BusinessRuleViolationException}.
     * После сохранения файл автоматически привязывается к профилю студента.</p>
     *
     * @param file файл резюме (PDF, макс. 5 МБ)
     * @return метаданные загруженного файла
     */
    @PostMapping(value = "/resume", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadResume(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        studentProfileService.requireStudentProfile(user.getId());
        FileUploadResponse response = fileStorage.store(file, FileType.RESUME, user.getId());
        studentProfileService.linkResume(user.getId(), response.id());
        return response;
    }

    /**
     * Загружает файл портфолио текущего пользователя.
     *
     * @param file файл портфолио
     * @return метаданные загруженного файла
     */
    @PostMapping(value = "/portfolio", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadPortfolio(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        return fileStorage.store(file, FileType.PORTFOLIO, user.getId());
    }

    /**
     * Загружает аватар текущего пользователя.
     *
     * <p>После сохранения файл автоматически привязывается к записи пользователя.</p>
     *
     * @param file изображение аватара (JPEG/PNG, макс. 2 МБ)
     * @return метаданные загруженного файла
     */
    @PostMapping(value = "/avatar", consumes = MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public FileUploadResponse uploadAvatar(@RequestParam("file") MultipartFile file) {
        User user = userService.getCurrentUserEntity();
        FileUploadResponse response = fileStorage.store(file, FileType.AVATAR, user.getId());
        userService.linkAvatar(user.getId(), response.id());
        return response;
    }
}
