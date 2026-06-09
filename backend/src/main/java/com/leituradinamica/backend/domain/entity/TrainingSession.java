package com.leituradinamica.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String exerciseKey;

    @Column(nullable = false, length = 180)
    private String exerciseName;

    @Column(nullable = false)
    private Integer targetWpm;

    @Column(nullable = false)
    private Integer achievedWpm;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Boolean completed;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}package com.leituradinamica.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "training_sessions")
public class TrainingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 80)
    private String exerciseKey;

    @Column(nullable = false, length = 180)
    private String exerciseName;

    @Column(nullable = false)
    private Integer targetWpm;

    @Column(nullable = false)
    private Integer achievedWpm;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Column(nullable = false)
    private Boolean completed;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}