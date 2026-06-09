package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Subscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}