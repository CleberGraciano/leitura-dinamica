package com.leituradinamica.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.leituradinamica.backend.domain.entity.User;
import com.leituradinamica.backend.domain.enums.Role;
import com.leituradinamica.backend.domain.enums.SubscriptionPlanType;
import com.leituradinamica.backend.repository.BookRepository;
import com.leituradinamica.backend.repository.ReadingSessionRepository;
import com.leituradinamica.backend.repository.UserAchievementRepository;
import com.leituradinamica.backend.service.StatsService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReadingSessionRepository readingSessionRepository;

    @Mock
    private UserAchievementRepository userAchievementRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void shouldBuildDashboardCards() {
        User user = User.builder().id(1L).name("Teste").email("teste@dev").password("x").role(Role.ROLE_USER).planType(SubscriptionPlanType.FREE).build();
        when(bookRepository.countByUserId(1L)).thenReturn(3L);
        when(readingSessionRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());
        when(userAchievementRepository.findByUserId(1L)).thenReturn(List.of());

        var dashboard = statsService.dashboard(user);

        assertThat(dashboard.cards()).hasSizeGreaterThanOrEqualTo(4);
    }
}