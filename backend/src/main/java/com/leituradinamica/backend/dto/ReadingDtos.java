package com.leituradinamica.backend.dto;

import com.leituradinamica.backend.domain.enums.ReadingSessionStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class ReadingDtos {

    private ReadingDtos() {
    }

    public record ReadingCommand(
            @NotNull Long bookId,
            @NotNull @Min(100) @Max(2000) Integer wpm,
            @NotNull @Min(0) Integer wordsRead,
            @NotNull @Min(0) Integer durationSeconds,
            @NotNull @Min(0) @Max(100) Double progress,
            @NotNull @Min(0) Integer wordPosition
    ) {
    }

    public record ReadingHistoryItem(
            Long id,
            Long bookId,
            String bookTitle,
            Integer wpm,
            Integer wordsRead,
            Integer durationSeconds,
            Double progress,
            ReadingSessionStatus status,
            Instant createdAt,
            Integer lastWordPosition
    ) {
    }

    public record ReadingHistoryResponse(List<ReadingHistoryItem> items) {
    }
}