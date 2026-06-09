package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Bookmark;
import com.leituradinamica.backend.domain.entity.TrainingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.dto.TrainingDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.BookmarkRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.TrainingSessionRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {

    private final TrainingSessionRepository trainingSessionRepository;
    private final BookRepository bookRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final AchievementService achievementService;

    public TrainingService(
            TrainingSessionRepository trainingSessionRepository,
            BookRepository bookRepository,
            BookmarkRepository bookmarkRepository,
            ReadingSessionRepository readingSessionRepository,
            AchievementService achievementService
    ) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.bookRepository = bookRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.achievementService = achievementService;
    }

    public TrainingDtos.TrainingOverviewResponse overview(User user) {
        List<TrainingSession> sessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        TrainingSnapshot snapshot = buildSnapshot(user, sessions);

        List<TrainingDtos.TrainingExerciseItem> exercises = exerciseDefinitions().stream()
                .map(definition -> {
                    long completedCount = sessions.stream()
                            .filter(item -> item.getExerciseKey().equals(definition.key()) && Boolean.TRUE.equals(item.getCompleted()))
                            .count();
                    Instant lastCompletedAt = sessions.stream()
                            .filter(item -> item.getExerciseKey().equals(definition.key()) && Boolean.TRUE.equals(item.getCompleted()))
                            .map(TrainingSession::getCreatedAt)
                            .findFirst()
                            .orElse(null);
                    return new TrainingDtos.TrainingExerciseItem(
                            definition.id(),
                            definition.key(),
                            definition.name(),
                            definition.description(),
                            definition.helper(),
                            definition.targetWpm(),
                            definition.unlockedWhen().test(snapshot),
                            completedCount,
                            lastCompletedAt
                    );
                })
                .toList();

        List<TrainingDtos.TrainingSessionItem> recentSessions = sessions.stream()
                .limit(8)
                .map(this::toSessionItem)
                .toList();

        int weeklyProgress = Math.min(100, Math.toIntExact(sessions.stream()
                .filter(item -> item.getCreatedAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS)))
                .count() * 20));

        return new TrainingDtos.TrainingOverviewResponse(exercises, recentSessions, weeklyProgress);
    }

    @Transactional
    public TrainingDtos.TrainingSessionItem execute(TrainingDtos.TrainingExecutionRequest request, User user) {
        validateSpeedLimit(user, request.achievedWpm());
        ExerciseDefinition definition = exerciseDefinitions().stream()
                .filter(item -> item.key().equals(request.exerciseKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Exercicio de treino nao encontrado."));

        TrainingSnapshot snapshot = buildSnapshot(user, trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()));
        if (!definition.unlockedWhen().test(snapshot)) {
            throw new BusinessException("Este exercicio ainda nao foi liberado para o usuario atual.");
        }

        TrainingSession session = trainingSessionRepository.save(TrainingSession.builder()
                .user(user)
                .exerciseKey(definition.key())
                .exerciseName(definition.name())
                .targetWpm(definition.targetWpm())
                .achievedWpm(request.achievedWpm())
                .durationSeconds(request.durationSeconds())
                .completed(Boolean.TRUE.equals(request.completed()))
                .build());

        achievementService.evaluateTraining(user, request.achievedWpm(), request.durationSeconds(), Boolean.TRUE.equals(request.completed()));
        return toSessionItem(session);
    }

    private TrainingDtos.TrainingSessionItem toSessionItem(TrainingSession session) {
        return new TrainingDtos.TrainingSessionItem(
                session.getId(),
                session.getExerciseKey(),
                session.getExerciseName(),
                session.getTargetWpm(),
                session.getAchievedWpm(),
                session.getDurationSeconds(),
                Boolean.TRUE.equals(session.getCompleted()),
                session.getCreatedAt()
        );
    }

    private TrainingSnapshot buildSnapshot(User user, List<TrainingSession> sessions) {
        long booksCount = bookRepository.countByUserId(user.getId());
        List<Bookmark> bookmarks = bookRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(book -> bookmarkRepository.findTopByUserIdAndBookIdOrderByCreatedAtDesc(user.getId(), book.getId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
        double averageProgress = bookmarks.isEmpty()
                ? 0
                : bookmarks.stream().mapToInt(Bookmark::getWordPosition).average().orElse(0);
        long completedReadings = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(item -> item.getProgress() >= 100)
                .count();
        long recentTrainings = sessions.stream()
                .filter(item -> item.getCreatedAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS)))
                .count();
        int peakWpm = Math.max(
                sessions.stream().mapToInt(TrainingSession::getAchievedWpm).max().orElse(0),
                readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream().mapToInt(item -> item.getWpm()).max().orElse(0)
        );
        long completedTrainingSessions = sessions.stream().filter(item -> Boolean.TRUE.equals(item.getCompleted())).count();

        return new TrainingSnapshot(booksCount, averageProgress, completedReadings, recentTrainings, peakWpm, completedTrainingSessions);
    }

    private List<ExerciseDefinition> exerciseDefinitions() {
        return List.of(
                new ExerciseDefinition(1L, "orp-inicial", "Foco ORP Inicial", "Treino curto para fixacao do ponto ideal de reconhecimento.", "Disponivel apos cadastrar pelo menos um livro.", 220, snapshot -> snapshot.booksCount() >= 1),
                new ExerciseDefinition(2L, "ritmo-300", "Ritmo de 300 WPM", "Sessao guiada para reduzir subvocalizacao e manter cadencia.", "Exige biblioteca ativa e algum progresso salvo.", 300, snapshot -> snapshot.booksCount() >= 1 && snapshot.averageProgress() > 0),
                new ExerciseDefinition(3L, "agrupamento", "Agrupamento de Palavras", "Treino com blocos de termos e mudanca de foco visual.", "Exige 3 sessoes recentes de treino ou uma leitura concluida.", 420, snapshot -> snapshot.recentTrainings() >= 3 || snapshot.completedReadings() >= 1),
                new ExerciseDefinition(4L, "compreensao", "Compreensao Acelerada", "Blocos de leitura com checkpoint de retencao.", "Exige ao menos uma leitura concluida.", 520, snapshot -> snapshot.completedReadings() >= 1),
                new ExerciseDefinition(5L, "sprint-avancado", "Sprint Avancado", "Treino de alta velocidade com meta de estabilidade.", "Exige pico de 500 WPM ou dois treinos concluidos.", 650, snapshot -> snapshot.peakWpm() >= 500 || snapshot.completedTrainingSessions() >= 2)
        );
    }

    private void validateSpeedLimit(User user, Integer wpm) {
        if (user.getPlanType() == SubscriptionPlanType.FREE && wpm > 500) {
            throw new BusinessException("Plano FREE permite ate 500 WPM nos exercicios.");
        }
    }

    private record ExerciseDefinition(
            Long id,
            String key,
            String name,
            String description,
            String helper,
            Integer targetWpm,
            Predicate<TrainingSnapshot> unlockedWhen
    ) {
    }

    private record TrainingSnapshot(
            long booksCount,
            double averageProgress,
            long completedReadings,
            long recentTrainings,
            int peakWpm,
            long completedTrainingSessions
    ) {
    }
}package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.TrainingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.dto.TrainingDtos;
import com.leituradinamica.backend.exception.BusinessException;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.BookmarkRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.TrainingSessionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrainingService {

    private static final int WEEKLY_TARGET = 5;

    private final TrainingSessionRepository trainingSessionRepository;
    private final BookRepository bookRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final AchievementService achievementService;

    public TrainingService(
            TrainingSessionRepository trainingSessionRepository,
            BookRepository bookRepository,
            BookmarkRepository bookmarkRepository,
            ReadingSessionRepository readingSessionRepository,
            AchievementService achievementService
    ) {
        this.trainingSessionRepository = trainingSessionRepository;
        this.bookRepository = bookRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.achievementService = achievementService;
    }

    public TrainingDtos.TrainingOverviewResponse overview(User user) {
        List<TrainingSession> sessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        List<ExerciseDefinition> definitions = buildDefinitions(user, sessions);
        long weeklyCompletedSessions = countWeeklyCompletedSessions(sessions);

        return new TrainingDtos.TrainingOverviewResponse(
                definitions.stream().map(definition -> definition.toDto(sessions)).toList(),
                Math.min(100, Math.toIntExact(Math.round((weeklyCompletedSessions / (double) WEEKLY_TARGET) * 100))),
                weeklyCompletedSessions,
                sessions.stream().limit(6).map(this::toSessionItem).toList()
        );
    }

    @Transactional
    public TrainingDtos.TrainingOverviewResponse execute(TrainingDtos.TrainingExecutionRequest request, User user) {
        List<TrainingSession> existingSessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        ExerciseDefinition definition = buildDefinitions(user, existingSessions).stream()
                .filter(item -> item.exerciseKey().equals(request.exerciseKey()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Exercicio de treino nao encontrado."));

        if (!definition.unlocked()) {
            throw new BusinessException("Este exercicio ainda esta bloqueado para o perfil atual.");
        }

        TrainingSession session = trainingSessionRepository.save(TrainingSession.builder()
                .user(user)
                .exerciseKey(definition.exerciseKey())
                .exerciseName(definition.name())
                .targetWpm(definition.targetWpm())
                .achievedWpm(request.achievedWpm())
                .durationSeconds(request.durationSeconds())
                .completed(Boolean.TRUE.equals(request.completed()))
                .build());

        achievementService.evaluateTraining(user, request.achievedWpm(), request.durationSeconds());
        List<TrainingSession> refreshedSessions = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        return overview(user);
    }

    private List<ExerciseDefinition> buildDefinitions(User user, List<TrainingSession> sessions) {
        long booksCount = bookRepository.countByUserId(user.getId());
        long completedBooks = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .filter(item -> item.getProgress() >= 100)
                .count();
        int peakWpm = sessions.stream().map(TrainingSession::getAchievedWpm).max(Comparator.naturalOrder()).orElse(0);
        int bookmarkPeakWpm = bookmarkRepository.findAll().stream()
                .filter(bookmark -> bookmark.getUser().getId().equals(user.getId()))
                .map(item -> item.getLastWpm())
                .max(Comparator.naturalOrder())
                .orElse(0);
        int currentPeakWpm = Math.max(peakWpm, bookmarkPeakWpm);
        long weeklyCompletedSessions = countWeeklyCompletedSessions(sessions);

        return List.of(
                new ExerciseDefinition("orp-focus", "Foco ORP Inicial", "Treino curto para fixacao do ponto ideal de reconhecimento.", "Disponivel apos cadastrar pelo menos um livro.", 220, booksCount >= 1),
                new ExerciseDefinition("rhythm-300", "Ritmo de 300 WPM", "Sessao guiada para reduzir subvocalizacao e manter cadencia.", "Exige ao menos um treino concluido ou 1 leitura iniciada.", 300, weeklyCompletedSessions >= 1 || booksCount >= 1),
                new ExerciseDefinition("grouping", "Agrupamento de Palavras", "Treino com blocos de termos e mudanca de foco visual.", "Exige 3 sessoes concluidas na semana.", 420, weeklyCompletedSessions >= 3),
                new ExerciseDefinition("comprehension", "Compreensao Acelerada", "Blocos de leitura com checkpoint de retencao.", "Exige pelo menos um livro concluido.", 520, completedBooks >= 1),
                new ExerciseDefinition("sprint-advanced", "Sprint Avancado", "Treino de alta velocidade com meta de estabilidade.", "Exige pico de 500 WPM em leitura ou treino.", 650, currentPeakWpm >= 500)
        );
    }

    private long countWeeklyCompletedSessions(List<TrainingSession> sessions) {
        Instant start = LocalDate.now().minusDays(6).atStartOfDay().toInstant(ZoneOffset.UTC);
        return sessions.stream()
                .filter(session -> Boolean.TRUE.equals(session.getCompleted()))
                .filter(session -> !session.getCreatedAt().isBefore(start))
                .count();
    }

    private TrainingDtos.TrainingSessionItem toSessionItem(TrainingSession session) {
        return new TrainingDtos.TrainingSessionItem(
                session.getId(),
                session.getExerciseKey(),
                session.getExerciseName(),
                session.getTargetWpm(),
                session.getAchievedWpm(),
                session.getDurationSeconds(),
                session.getCompleted(),
                session.getCreatedAt()
        );
    }

    private record ExerciseDefinition(
            String exerciseKey,
            String name,
            String description,
            String helper,
            int targetWpm,
            boolean unlocked
    ) {
        private TrainingDtos.TrainingExerciseItem toDto(List<TrainingSession> sessions) {
            List<TrainingSession> matchingSessions = sessions.stream()
                    .filter(session -> session.getExerciseKey().equals(exerciseKey))
                    .toList();
            Optional<TrainingSession> latestSession = matchingSessions.stream().max(Comparator.comparing(TrainingSession::getCreatedAt));
            Integer bestWpm = matchingSessions.stream().map(TrainingSession::getAchievedWpm).max(Comparator.naturalOrder()).orElse(null);
            long completedCount = matchingSessions.stream().filter(session -> Boolean.TRUE.equals(session.getCompleted())).count();

            return new TrainingDtos.TrainingExerciseItem(
                    exerciseKey,
                    name,
                    description,
                    helper,
                    targetWpm,
                    unlocked,
                    completedCount,
                    bestWpm,
                    latestSession.map(TrainingSession::getCreatedAt).orElse(null)
            );
        }
    }
}