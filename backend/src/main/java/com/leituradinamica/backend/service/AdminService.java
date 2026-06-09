package com.leituradinamica.backend.service;

import com.leituradinamica.backend.dto.StatsDtos;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.SubscriptionRepository;
import com.leituradinamica.backend.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AdminService(
            UserRepository userRepository,
            BookRepository bookRepository,
            ReadingSessionRepository readingSessionRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.readingSessionRepository = readingSessionRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    public StatsDtos.AdminDashboardResponse dashboard() {
        double averageSpeed = readingSessionRepository.findAll().stream().mapToInt(item -> item.getWpm()).average().orElse(0);
        return new StatsDtos.AdminDashboardResponse(List.of(
                new StatsDtos.MetricItem("Usuários ativos", String.valueOf(userRepository.count()), "Base cadastrada"),
                new StatsDtos.MetricItem("Livros públicos", String.valueOf(bookRepository.findAll().stream().filter(item -> item.isPublicBook()).count()), "Catálogo compartilhado"),
                new StatsDtos.MetricItem("WPM médio global", String.format("%.0f", averageSpeed), "Todos os usuários"),
                new StatsDtos.MetricItem("Assinaturas", String.valueOf(subscriptionRepository.count()), "Planos registrados")
        ));
    }
}