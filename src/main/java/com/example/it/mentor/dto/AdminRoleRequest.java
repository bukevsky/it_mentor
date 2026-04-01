package com.example.it.mentor.dto;

import com.example.it.mentor.entity.RoleCode;
import jakarta.validation.constraints.NotNull;

public record AdminRoleRequest(@NotNull RoleCode role) {}
