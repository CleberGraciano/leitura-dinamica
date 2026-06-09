package com.leituradinamica.backend.dto;

import java.time.Instant;
import java.util.List;

public final class AchievementDtos {

    private AchievementDtos() {
    }

    public record AchievementItem(
            Long id,
            String name,
            String description,
            String badge,
            boolean unlocked,
            Instant achievedAt
    ) {
    }

    public record AchievementResponse(List<AchievementItem> items) {
    }
}package com.leituradinamica.backend.dto;

import java.time.Instant;
import java.util.List;

public final class AchievementDtos {

    private AchievementDtos() {
    }

    public record AchievementItem(
            Long id,
            String name,
            String description,
            String badge,
            boolean unlocked,
            Instant achievedAt
    ) {
    }

    public record AchievementResponse(List<AchievementItem> items) {
    }
}