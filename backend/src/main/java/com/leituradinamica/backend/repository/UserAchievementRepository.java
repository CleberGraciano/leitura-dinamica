package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.UserAchievement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserId(Long userId);
}