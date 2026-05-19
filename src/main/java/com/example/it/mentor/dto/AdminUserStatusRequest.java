package com.example.it.mentor.dto;

import com.example.it.mentor.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUserStatusRequest(@NotNull UserStatus status) {}
