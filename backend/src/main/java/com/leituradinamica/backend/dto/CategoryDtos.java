package com.leituradinamica.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDtos {

    private CategoryDtos() {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 120) String name,
            @Size(max = 1000) String description
    ) {
    }

    public record CategoryResponse(Long id, String name, String description) {
    }
}