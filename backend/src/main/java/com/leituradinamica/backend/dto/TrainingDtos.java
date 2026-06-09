package com.leituradinamica.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public final class TrainingDtos {

    private TrainingDtos() {
    }

    public record TrainingExecutionRequest(
            @NotBlank String exerciseKey,
            @NotNull @Min(100) @Max(2000) Integer achievedWpm,
            @NotNull @Min(1) Integer durationSeconds,
            @NotNull Boolean completed
    ) {
    }

    public record TrainingExerciseItem(
            Long id,
            String key,
            String name,
            String description,
            String helper,
            Integer targetWpm,
            boolean unlocked,
            long completedCount,
            Instant lastCompletedAt
    ) {
    }

    public record TrainingSessionItem(
            Long id,
            String exerciseKey,
            String exerciseName,
            Integer targetWpm,
            Integer achievedWpm,
            Integer durationSeconds,
            boolean completed,
            Instant createdAt
    ) {
    }

    public record TrainingOverviewResponse(
            List<TrainingExerciseItem> exercises,
            List<TrainingSessionItem> recentSessions,
            int weeklyProgress
    ) {
    }
}