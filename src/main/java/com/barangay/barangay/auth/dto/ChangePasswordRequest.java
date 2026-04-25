package com.barangay.barangay.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Email
        String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters")
        String newPassword,
        @NotBlank
        String confirmPassword,

        String username
) {}