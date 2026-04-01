package com.example.it.mentor.controller;

import com.example.it.mentor.dto.AdminRoleRequest;
import com.example.it.mentor.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> assignRole(@PathVariable Long userId,
                                           @Valid @RequestBody AdminRoleRequest request) {
        adminService.assignRole(userId, request.role());
        return ResponseEntity.ok().build();
    }
}
