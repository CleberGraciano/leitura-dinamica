package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Flashcard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
    List<Flashcard> findByUserIdAndBookId(Long userId, Long bookId);
}