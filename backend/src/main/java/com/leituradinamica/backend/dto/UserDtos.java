package com.leituradinamica.backend.dto;

import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserResponse(
            Long id,
            String name,
            String email,
            String photo,
            Role role,
            Instant createdAt,
            boolean active,
            SubscriptionPlanType planType
    ) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(min = 3, max = 120) String name,
            @Email @NotBlank String email,
            String photo,
            Boolean active,
            Role role,
            SubscriptionPlanType planType
    ) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 60) String newPassword
    ) {
    }
}