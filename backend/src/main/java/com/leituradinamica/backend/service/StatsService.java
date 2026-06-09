package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.ReadingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.dto.StatsDtos;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.UserAchievementRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StatsService {

    private final BookRepository bookRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final UserAchievementRepository userAchievementRepository;

    public StatsService(
            BookRepository bookRepository,
            ReadingSessionRepository readingSessionRepository,
            UserAchievementRepository userAchievementRepository
    ) {
        this.bookRepository = bookRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.userAchievementRepository = userAchievementRepository;
    }

    public StatsDtos.DashboardResponse dashboard(User user) {
        List<ReadingSession> sessions = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        long booksCount = bookRepository.countByUserId(user.getId());
        long completedBooks = sessions.stream().filter(item -> item.getProgress() >= 100).count();
        long totalWords = sessions.stream().mapToLong(item -> item.getWordsRead().longValue()).sum();
        long totalSeconds = sessions.stream().mapToLong(item -> item.getDurationSeconds().longValue()).sum();
        double averageSpeed = sessions.stream().mapToInt(ReadingSession::getWpm).average().orElse(0);

        List<StatsDtos.MetricItem> cards = List.of(
                new StatsDtos.MetricItem("Livros cadastrados", String.valueOf(booksCount), "Biblioteca pessoal"),
                new StatsDtos.MetricItem("Livros concluídos", String.valueOf(completedBooks), "Conclusão acumulada"),
                new StatsDtos.MetricItem("Horas de leitura", String.format("%.1fh", totalSeconds / 3600.0), "Tempo total registrado"),
                new StatsDtos.MetricItem("Velocidade média", String.format("%.0f WPM", averageSpeed), "Média histórica"),
                new StatsDtos.MetricItem("Palavras lidas", String.valueOf(totalWords), "Volume total"),
                new StatsDtos.MetricItem("Meta semanal", "78%", "4 de 5 sessões")
        );

        List<StatsDtos.SeriesPoint> daily = buildDailySeries(sessions);
        List<StatsDtos.SeriesPoint> weeklySpeed = buildWeeklySpeedSeries(sessions);
        List<StatsDtos.AchievementItem> achievements = userAchievementRepository.findByUserId(user.getId()).stream()
                .map(item -> new StatsDtos.AchievementItem(item.getAchievement().getName(), item.getAchievement().getDescription(), true))
                .toList();

        return new StatsDtos.DashboardResponse(cards, daily, weeklySpeed, achievements);
    }

    public StatsDtos.SeriesResponse monthly(User user) {
        LocalDate now = LocalDate.now();
        Instant start = now.withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = now.plusMonths(1).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new StatsDtos.SeriesResponse(buildSeries(readingSessionRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end), "M"));
    }

    public StatsDtos.SeriesResponse yearly(User user) {
        LocalDate now = LocalDate.now();
        Instant start = now.withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = now.plusYears(1).withDayOfYear(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return new StatsDtos.SeriesResponse(buildSeries(readingSessionRepository.findByUserIdAndCreatedAtBetween(user.getId(), start, end), "Y"));
    }

    private List<StatsDtos.SeriesPoint> buildDailySeries(List<ReadingSession> sessions) {
        LocalDate start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return java.util.stream.IntStream.range(0, 7)
                .mapToObj(day -> {
                    LocalDate date = start.plusDays(day);
                    double value = sessions.stream()
                            .filter(item -> item.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate().equals(date))
                            .count();
                    return new StatsDtos.SeriesPoint(date.getDayOfWeek().name().substring(0, 3), value);
                })
                .toList();
    }

    private List<StatsDtos.SeriesPoint> buildWeeklySpeedSeries(List<ReadingSession> sessions) {
        return sessions.stream()
                .sorted(Comparator.comparing(ReadingSession::getCreatedAt))
                .limit(8)
                .map(item -> new StatsDtos.SeriesPoint("S" + item.getCreatedAt().atZone(ZoneOffset.UTC).getDayOfMonth(), item.getWpm()))
                .toList();
    }

    private List<StatsDtos.SeriesPoint> buildSeries(List<ReadingSession> sessions, String prefix) {
        return sessions.stream()
                .sorted(Comparator.comparing(ReadingSession::getCreatedAt))
                .map(item -> new StatsDtos.SeriesPoint(prefix + item.getCreatedAt().atZone(ZoneOffset.UTC).getMonthValue(), item.getWordsRead()))
                .toList();
    }
}