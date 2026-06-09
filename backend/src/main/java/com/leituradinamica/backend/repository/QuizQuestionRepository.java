package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.QuizQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizId(Long quizId);
}