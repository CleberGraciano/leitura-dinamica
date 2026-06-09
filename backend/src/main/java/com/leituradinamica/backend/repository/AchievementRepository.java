package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Achievement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    Optional<Achievement> findByName(String name);
}