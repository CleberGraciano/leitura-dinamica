package com.leituradinamica.backend.dto;

import com.leituradinamica.backend.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 120) String name,
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 60) String password,
            String photo,
            Role role
    ) {
    }

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {
    }

    public record RefreshTokenRequest(@NotBlank String refreshToken) {
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            UserDtos.UserResponse user
    ) {
    }
}