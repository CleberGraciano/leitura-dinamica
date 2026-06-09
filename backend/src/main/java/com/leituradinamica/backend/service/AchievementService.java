package com.leituradinamica.backend.service;

import com.leituradinamica.backend.domain.entity.Achievement;
import com.leituradinamica.backend.domain.entity.TrainingSession;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.entity.UserAchievement;
import com.leituradinamica.backend.dto.AchievementDtos;
import com.leituradinamica.backend.repository.AchievementRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.TrainingSessionRepository;
import com.leituradinamica.backend.repository.UserAchievementRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final TrainingSessionRepository trainingSessionRepository;

    public AchievementService(
            AchievementRepository achievementRepository,
            UserAchievementRepository userAchievementRepository,
            ReadingSessionRepository readingSessionRepository,
            TrainingSessionRepository trainingSessionRepository
    ) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.trainingSessionRepository = trainingSessionRepository;
    }

    @Transactional
    public void evaluate(User user, int wpm, double progress, int durationSeconds) {
        Set<String> unlocked = userAchievementRepository.findByUserId(user.getId()).stream()
                .map(item -> item.getAchievement().getName())
                .collect(java.util.stream.Collectors.toSet());

        if (progress >= 100) {
            unlockIfNecessary(user, unlocked, "Primeira leitura");
        }
        if (wpm >= 800) {
            unlockIfNecessary(user, unlocked, "Mestre da velocidade");
        }
        long totalDuration = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .mapToLong(item -> item.getDurationSeconds().longValue())
                .sum() + durationSeconds;
        if (totalDuration >= 36000) {
            unlockIfNecessary(user, unlocked, "Maratonista");
        }

        long recentReadingSessions = readingSessionRepository.findByUserIdAndCreatedAtBetween(
                user.getId(),
                Instant.now().minus(7, ChronoUnit.DAYS),
                Instant.now()
        ).size();
        if (recentReadingSessions >= 3) {
            unlockIfNecessary(user, unlocked, "Leitor dedicado");
        }
    }

    @Transactional
    public void evaluateTraining(User user, int wpm, int durationSeconds, boolean completed) {
        Set<String> unlocked = userAchievementRepository.findByUserId(user.getId()).stream()
                .map(item -> item.getAchievement().getName())
                .collect(java.util.stream.Collectors.toSet());

        if (completed) {
            long completedSessions = trainingSessionRepository.countByUserIdAndCompletedTrue(user.getId());
            if (completedSessions >= 3) {
                unlockIfNecessary(user, unlocked, "Leitor dedicado");
            }
        }

        if (wpm >= 800) {
            unlockIfNecessary(user, unlocked, "Mestre da velocidade");
        }

        long readingDuration = readingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .mapToLong(item -> item.getDurationSeconds().longValue())
                .sum();
        long trainingDuration = trainingSessionRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .mapToLong(item -> item.getDurationSeconds().longValue())
                .sum() + durationSeconds;
        if (readingDuration + trainingDuration >= 36000) {
            unlockIfNecessary(user, unlocked, "Maratonista");
        }
    }

    public AchievementDtos.AchievementResponse listForUser(User user) {
        Map<Long, UserAchievement> unlockedByAchievementId = userAchievementRepository.findByUserId(user.getId()).stream()
                .collect(java.util.stream.Collectors.toMap(item -> item.getAchievement().getId(), item -> item, (left, right) -> left));

        return new AchievementDtos.AchievementResponse(
                achievementRepository.findAll().stream()
                        .map(achievement -> new AchievementDtos.AchievementItem(
                                achievement.getId(),
                                achievement.getName(),
                                achievement.getDescription(),
                                achievement.getBadge(),
                                unlockedByAchievementId.containsKey(achievement.getId()),
                                unlockedByAchievementId.containsKey(achievement.getId()) ? unlockedByAchievementId.get(achievement.getId()).getAchievedAt() : null
                        ))
                        .toList()
        );
    }

    private void unlockIfNecessary(User user, Set<String> unlocked, String achievementName) {
        if (unlocked.contains(achievementName)) {
            return;
        }
        achievementRepository.findByName(achievementName).ifPresent(achievement ->
                userAchievementRepository.save(UserAchievement.builder()
                        .user(user)
                        .achievement(achievement)
                        .build()));
    }
}