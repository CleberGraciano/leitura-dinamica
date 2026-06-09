package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.TrainingSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TrainingSession> findByUserIdAndCreatedAtBetween(Long userId, Instant start, Instant end);
    long countByUserIdAndCompletedTrue(Long userId);
    Optional<TrainingSession> findTopByUserIdAndExerciseKeyOrderByCreatedAtDesc(Long userId, String exerciseKey);
}package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.TrainingSession;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingSessionRepository extends JpaRepository<TrainingSession, Long> {
    List<TrainingSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<TrainingSession> findByUserIdAndCreatedAtBetween(Long userId, Instant start, Instant end);
}