package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Bookmark;
import com.leituradinamica.backend.domain.entity.TrainingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.Role;
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
                if (user.getRole() == Role.ROLE_ADMIN || user.getRole() == Role.ROLE_EDITOR) {
                        return;
                }

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
}