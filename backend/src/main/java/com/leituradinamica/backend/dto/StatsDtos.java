package com.leituradinamica.backend.dto;

import java.util.List;

public final class StatsDtos {

    private StatsDtos() {
    }

    public record MetricItem(String label, String value, String helper) {
    }

    public record SeriesPoint(String label, double value) {
    }

    public record AchievementItem(String name, String description, boolean unlocked) {
    }

    public record DashboardResponse(
            List<MetricItem> cards,
            List<SeriesPoint> readingsByDay,
            List<SeriesPoint> averageSpeedByWeek,
            List<AchievementItem> achievements
    ) {
    }

    public record SeriesResponse(List<SeriesPoint> items) {
    }

    public record AdminDashboardResponse(List<MetricItem> metrics) {
    }
}