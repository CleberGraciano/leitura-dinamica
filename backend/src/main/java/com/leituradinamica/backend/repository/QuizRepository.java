package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Quiz;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    List<Quiz> findByBookId(Long bookId);
}