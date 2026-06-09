package com.leituradinamica.backend.domain.entity;

import com.leituradinamica.backend.domain.enums.BookFileType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String author;

    @Column(length = 2000)
    private String description;

    private String cover;

    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookFileType fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean favorite = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean archived = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean publicBook = false;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String contentText;
}