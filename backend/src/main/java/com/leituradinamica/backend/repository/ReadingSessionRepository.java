package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.ReadingSession;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadingSessionRepository extends JpaRepository<ReadingSession, Long> {
    List<ReadingSession> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReadingSession> findTopByUserIdAndBookIdOrderByCreatedAtDesc(Long userId, Long bookId);
    List<ReadingSession> findByUserIdAndCreatedAtBetween(Long userId, Instant start, Instant end);
}