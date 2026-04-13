package com.example.it.mentor.controller;

import com.example.it.mentor.dto.AdminRoleRequest;
import com.example.it.mentor.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST-контроллер административных операций над пользователями.
 *
 * <p>Эндпоинты этого контроллера доступны только пользователям с ролью {@code ADMIN}
 * и используются для смены прикладной роли пользователя между {@code STUDENT} и
 * {@code MENTOR}.</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Назначает пользователю новую прикладную роль.
     *
     * <p>Смена роли {@code ADMIN} через этот эндпоинт запрещена. При успешном
     * переключении сервис гарантирует наличие соответствующего профиля.</p>
     *
     * @param userId идентификатор пользователя, которому меняется роль
     * @param request запрос с целевой ролью
     * @return пустой ответ со статусом {@code 200 OK}
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId,
                                           @Valid @RequestBody AdminRoleRequest request) {
        adminService.assignRole(userId, request.role());
        return ResponseEntity.ok().build();
    }
}
