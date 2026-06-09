package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);
}