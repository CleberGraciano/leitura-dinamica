import 'chart.js/auto';

import { NgClass } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { ChartConfiguration } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';

import { PlatformService } from '../data/platform.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [NgClass, BaseChartDirective],
  template: `
    <section class="dashboard-grid">
      <div class="hero card-surface">
        <div>
          <span class="hero-tag">{{ heroTag() }}</span>
          <h2>{{ heroTitle() }}</h2>
          <p>{{ heroCopy() }}</p>
        </div>

        <div class="hero-metrics">
          @for (card of cards(); track card.label) {
            <article>
              <strong>{{ card.value }}</strong>
              <span>{{ card.label }}</span>
              <small>{{ card.helper }}</small>
            </article>
          }
        </div>
      </div>

      <section class="card-surface chart-card">
        <header class="card-head">
          <h3 class="section-title">Livros por categoria</h3>
        </header>
        <canvas baseChart [type]="readingsChart().type" [data]="readingsChart().data" [options]="readingsChart().options"></canvas>
      </section>

      <section class="card-surface chart-card">
        <header class="card-head">
          <h3 class="section-title">Volume textual por formato</h3>
        </header>
        <canvas baseChart [type]="speedChart().type" [data]="speedChart().data" [options]="speedChart().options"></canvas>
      </section>

      <section class="card-surface list-card">
        <header class="card-head">
          <h3 class="section-title">Biblioteca em andamento</h3>
        </header>
        <div class="book-list">
          @for (book of books(); track book.id) {
            <article>
              <div>
                <h3>{{ book.title }}</h3>
                <p>{{ book.author }} • {{ book.category }}</p>
              </div>

              <div class="book-actions">
                <span class="status-pill" [ngClass]="book.progress > 70 ? 'success' : 'warning'">{{ book.progress }}% lido</span>
                <button class="favorite-button" type="button" (click)="toggleFavorite(book.id)">{{ book.favorite ? '♥' : '♡' }}</button>
              </div>
            </article>
          }
        </div>
      </section>

      <section class="card-surface list-card accent-card">
        <header class="card-head">
          <h3 class="section-title">Próxima meta</h3>
        </header>
        <div class="goal-box">
          <span class="goal-symbol">↗</span>
          <div>
            <strong>{{ nextGoalTitle() }}</strong>
            <p>{{ nextGoalCopy() }}</p>
          </div>
        </div>
      </section>
    </section>
  `,
  styles: [`
    .dashboard-grid {
      display: grid;
      gap: 1.5rem;
      grid-template-columns: repeat(12, minmax(0, 1fr));
    }

    .hero,
    .chart-card,
    .list-card {
      padding: 1.4rem;
      border-radius: 24px;
    }

    .card-head {
      margin-bottom: 1rem;
    }

    .hero {
      grid-column: span 12;
      display: grid;
      gap: 1.5rem;
      grid-template-columns: 1.3fr 1fr;
    }

    .hero h2 {
      margin: 0.5rem 0 0.75rem;
      font-size: clamp(2rem, 4vw, 3.5rem);
      line-height: 0.95;
      max-width: 14ch;
    }

    .hero p,
    .hero-metrics small,
    .book-list p,
    .goal-box p {
      color: var(--ld-muted);
    }

    .hero-tag {
      display: inline-flex;
      padding: 0.45rem 0.85rem;
      border-radius: 999px;
      background: rgba(0, 168, 150, 0.13);
      color: var(--ld-accent-2);
      font-weight: 700;
    }

    .hero-metrics {
      display: grid;
      gap: 1rem;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .hero-metrics article {
      padding: 1rem;
      border-radius: 20px;
      background: rgba(16, 42, 67, 0.05);
      display: grid;
      gap: 0.25rem;
    }

    .hero-metrics strong {
      font-size: 1.8rem;
    }

    .chart-card,
    .list-card {
      grid-column: span 6;
    }

    .book-list {
      display: grid;
      gap: 1rem;
    }

    .book-list article {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid rgba(16, 42, 67, 0.08);
    }

    .book-list article:last-child {
      border-bottom: 0;
      padding-bottom: 0;
    }

    .book-list h3 {
      margin: 0;
      font-size: 1rem;
    }

    .book-actions {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .accent-card {
      background: linear-gradient(135deg, rgba(255, 122, 24, 0.18), rgba(0, 168, 150, 0.16));
    }

    .goal-box {
      display: flex;
      gap: 1rem;
      align-items: center;
    }

    .goal-symbol {
      display: inline-flex;
      width: 3rem;
      height: 3rem;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.45);
      font-size: 2rem;
      color: var(--ld-accent);
    }

    .favorite-button {
      font-size: 1.4rem;
      color: var(--ld-accent);
      cursor: pointer;
    }

    @media (max-width: 992px) {
      .hero,
      .chart-card,
      .list-card {
        grid-column: span 12;
      }

      .hero {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class DashboardPageComponent {
  private readonly platformService = inject(PlatformService);

  protected readonly cards = this.platformService.cards;
  protected readonly books = this.platformService.activeBooks;
  protected readonly currentUser = this.platformService.currentUser;
  protected readonly roleLabel = computed(() => {
    const role = this.currentUser()?.role ?? 'ROLE_USER';
    return role === 'ROLE_ADMIN' ? 'Administrador' : role === 'ROLE_EDITOR' ? 'Editor' : 'Usuario';
  });
  protected readonly heroTag = computed(() => `Acesso ${this.roleLabel()}`);
  protected readonly heroTitle = computed(() => {
    const userName = this.currentUser()?.name?.split(' ')[0] ?? 'Leitor';
    return `${userName}, acompanhe sua biblioteca real e o impacto do seu perfil dentro da plataforma.`;
  });
  protected readonly heroCopy = computed(() => {
    const planType = this.currentUser()?.planType ?? 'FREE';
    const planLabel = planType === 'PREMIUM_YEARLY' ? 'Premium Yearly' : planType === 'PREMIUM_MONTHLY' ? 'Premium Monthly' : 'Free';
    return `Seu painel agora usa livros persistidos por usuario autenticado. Perfil atual: ${this.roleLabel()} • Plano ${planLabel}.`;
  });
  protected readonly nextGoalTitle = computed(() => {
    const books = this.books();
    if (books.length === 0) {
      return 'Cadastrar o primeiro livro com upload ou texto manual';
    }

    const incompleteBook = books.find((book) => book.progress < 100);
    if (incompleteBook) {
      return `Avancar a leitura de ${incompleteBook.title} para consolidar progresso real`;
    }

    return 'Expandir a biblioteca com novos titulos e categorias';
  });
  protected readonly nextGoalCopy = computed(() => {
    const favoriteBooks = this.platformService.favoriteBooks().length;
    const publicBooks = this.books().filter((book) => book.publicBook).length;
    return `${favoriteBooks} favoritos ativos e ${publicBooks} livros publicos disponiveis no seu catalogo atual.`;
  });

  protected readonly readingsChart = computed<ChartConfiguration<'bar'>>(() => ({
    type: 'bar',
    data: {
      labels: this.platformService.weeklyReadings().map((point) => point.label),
      datasets: [{
        label: 'Livros',
        data: this.platformService.weeklyReadings().map((point) => point.value),
        borderRadius: 12,
        backgroundColor: '#ff7a18'
      }]
    },
    options: {
      responsive: true,
      plugins: { legend: { display: false } },
      scales: { y: { beginAtZero: true } }
    }
  }));

  protected readonly speedChart = computed<ChartConfiguration<'line'>>(() => ({
    type: 'line',
    data: {
      labels: this.platformService.weeklySpeed().map((point) => point.label),
      datasets: [{
        label: 'Palavras mapeadas',
        data: this.platformService.weeklySpeed().map((point) => point.value),
        borderColor: '#00a896',
        backgroundColor: 'rgba(0, 168, 150, 0.18)',
        fill: true,
        tension: 0.35
      }]
    },
    options: {
      responsive: true,
      plugins: { legend: { display: false } }
    }
  }));

  toggleFavorite(bookId: number): void {
    this.platformService.toggleFavorite(bookId);
  }
}