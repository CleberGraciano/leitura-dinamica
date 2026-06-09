package com.leituradinamica.backend.bootstrap;

import com.leituradinamica.backend.domain.entity.Achievement;
import com.leituradinamica.backend.domain.entity.Book;
import com.leituradinamica.backend.domain.entity.Category;
import com.leituradinamica.backend.domain.entity.Subscription;
import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.BookFileType;
import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.domain.enums.SubscriptionStatus;
import com.leituradinamica.backend.repository.AchievementRepository;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.CategoryRepository;
import com.leituradinamica.backend.repository.SubscriptionRepository;
import com.leituradinamica.backend.repository.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final AchievementRepository achievementRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CategoryRepository categoryRepository,
            BookRepository bookRepository,
            AchievementRepository achievementRepository,
            SubscriptionRepository subscriptionRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.bookRepository = bookRepository;
        this.achievementRepository = achievementRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = userRepository.save(User.builder()
                .name("Admin Leitura")
                .email("admin@leituradinamica.dev")
                .password(passwordEncoder.encode("Admin123!"))
                .role(Role.ROLE_ADMIN)
                .planType(SubscriptionPlanType.PREMIUM_YEARLY)
                .active(true)
                .build());

        User demo = userRepository.save(User.builder()
                .name("Usuário Demo")
                .email("demo@leituradinamica.dev")
                .password(passwordEncoder.encode("Demo12345"))
                .role(Role.ROLE_USER)
                .planType(SubscriptionPlanType.PREMIUM_MONTHLY)
                .active(true)
                .build());

        subscriptionRepository.saveAll(List.of(
                Subscription.builder().user(admin).planType(SubscriptionPlanType.PREMIUM_YEARLY).status(SubscriptionStatus.ACTIVE).paymentProvider("LOCAL").startDate(Instant.now()).expirationDate(Instant.now().plus(365, ChronoUnit.DAYS)).build(),
                Subscription.builder().user(demo).planType(SubscriptionPlanType.PREMIUM_MONTHLY).status(SubscriptionStatus.ACTIVE).paymentProvider("LOCAL").startDate(Instant.now()).expirationDate(Instant.now().plus(30, ChronoUnit.DAYS)).build()
        ));

        Category programacao = categoryRepository.save(Category.builder().name("Programação").description("Arquitetura, APIs e boas práticas.").build());
        Category historia = categoryRepository.save(Category.builder().name("História").description("Contexto, sociedade e evolução.").build());
        Category financas = categoryRepository.save(Category.builder().name("Finanças").description("Planejamento, investimento e orçamento.").build());

        bookRepository.saveAll(List.of(
                Book.builder().title("Spring Boot em Ação").author("Ana Campos").description("Guia prático para APIs modernas.").fileType(BookFileType.PDF).category(programacao).user(demo).favorite(true).contentText("Programação Java Spring Boot APIs REST segurança testes desempenho produtividade compreensão leitura dinâmica foco evolução.").build(),
                Book.builder().title("História da Inovação").author("Carlos Mendes").description("Mudanças de paradigma ao longo dos séculos.").fileType(BookFileType.EPUB).category(historia).user(demo).publicBook(true).contentText("Inovação ciência indústria criatividade colaboração conhecimento sociedade impacto aprendizagem contexto.").build(),
                Book.builder().title("Finanças para Crescer").author("Marina Sol").description("Princípios de saúde financeira.").fileType(BookFileType.TXT).category(financas).user(demo).favorite(true).contentText("Finanças orçamento reserva investimento juros compostos disciplina recorrência metas clareza crescimento sustentável.").build()
        ));

        achievementRepository.saveAll(List.of(
                Achievement.builder().name("Primeira leitura").description("Concluir o primeiro livro.").badge("first-read").build(),
                Achievement.builder().name("Leitor dedicado").description("Ler 7 dias consecutivos.").badge("dedicated-reader").build(),
                Achievement.builder().name("Mestre da velocidade").description("Ultrapassar 800 WPM.").badge("speed-master").build(),
                Achievement.builder().name("Maratonista").description("Ler mais de 10 horas.").badge("marathon-reader").build()
        ));
    }
}