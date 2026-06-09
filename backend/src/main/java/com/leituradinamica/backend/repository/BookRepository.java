package com.leituradinamica.backend.repository;

import com.leituradinamica.backend.domain.entity.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}