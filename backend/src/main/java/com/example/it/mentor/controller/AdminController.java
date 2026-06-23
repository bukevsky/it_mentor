package com.example.it.mentor.controller;

import com.example.it.mentor.dto.AdminRoleRequest;
import com.example.it.mentor.dto.AdminUserStatusRequest;
import com.example.it.mentor.dto.PagedResponse;
import com.example.it.mentor.dto.admin.AdminUserResponse;
import com.example.it.mentor.dto.admin.AdminUsersStatsResponse;
import com.example.it.mentor.entity.RoleCode;
import com.example.it.mentor.entity.UserStatus;
import com.example.it.mentor.service.AdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Validated
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Административные операции")
public class AdminController {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "email", "status");

    private final AdminService adminService;

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId,
                                           @Valid @RequestBody AdminRoleRequest request) {
        adminService.assignRole(userId, request.role());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<Void> changeUserStatus(@PathVariable Long userId,
                                                 @Valid @RequestBody AdminUserStatusRequest request) {
        adminService.changeUserStatus(userId, request.status());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public PagedResponse<AdminUserResponse> getUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) RoleCode role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        String[] parts = sort.split(",");
        String rawField = parts[0];
        String field = ALLOWED_SORT_FIELDS.contains(rawField) ? rawField : "createdAt";
        Sort.Direction dir = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        return adminService.getUsers(q, role, status,
                PageRequest.of(page, size, Sort.by(dir, field)));
    }

    @GetMapping("/users/stats")
    public AdminUsersStatsResponse getUsersStats() {
        return adminService.getUsersStats();
    }
}
