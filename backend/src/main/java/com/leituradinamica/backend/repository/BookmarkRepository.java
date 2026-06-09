package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Bookmark;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findTopByUserIdAndBookIdOrderByCreatedAtDesc(Long userId, Long bookId);
}