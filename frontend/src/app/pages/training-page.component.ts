import { Component, computed, inject } from '@angular/core';

import { PlatformService } from '../data/platform.service';

@Component({
  selector: 'app-training-page',
  standalone: true,
  template: `
    <section class="training-grid">
      <section class="card-surface training-card full-width">
        <header class="card-head">
          <h2 class="section-title">Plano progressivo de velocidade</h2>
        </header>
        <div class="timeline">
          @for (week of trainingPlan(); track week.week) {
            <article>
              <span>Semana {{ week.week }}</span>
              <strong>{{ week.targetWpm }} WPM</strong>
              <p>{{ week.stage }} • {{ week.focus }}</p>
            </article>
          }
        </div>
      </section>

      <section class="card-surface training-card">
        <header class="card-head">
          <h2 class="section-title">Exercicios desbloqueados</h2>
        </header>
        @for (exercise of trainingExercises(); track exercise.id) {
          <div class="achievement-row">
            <div>
              <strong>{{ exercise.name }}</strong>
              <p>{{ exercise.description }}</p>
              <small>{{ exercise.helper }}</small>
            </div>
            <span class="status-pill" [class.success]="exercise.unlocked" [class.muted]="!exercise.unlocked">{{ exercise.unlocked ? 'Liberado' : 'Bloqueado' }}</span>
          </div>
        }
      </section>

      <section class="card-surface training-card">
        <header class="card-head">
          <h2 class="section-title">Conquistas</h2>
        </header>
        @for (achievement of achievements(); track achievement.id) {
          <div class="achievement-row">
            <div>
              <strong>{{ achievement.name }}</strong>
              <p>{{ achievement.description }}</p>
            </div>
            <span class="status-pill" [class.success]="achievement.unlocked" [class.muted]="!achievement.unlocked">{{ achievement.unlocked ? 'Desbloqueada' : 'Pendente' }}</span>
          </div>
        }
      </section>

      <section class="card-surface training-card full-width">
        <header class="card-head">
          <h2 class="section-title">Meta semanal</h2>
        </header>
        <div class="goal-stack">
          <strong>{{ completedExercises() }} exercicios liberados na trilha atual</strong>
          <div class="progress-ui"><span [style.width.%]="weeklyProgress()"></span></div>
          <p>{{ weeklyProgress() }}% da meta semanal concluida com base nas sessoes recentes e no progresso real da biblioteca.</p>
        </div>
      </section>
    </section>
  `,
  styles: [`
    .training-grid {
      display: grid;
      grid-template-columns: repeat(12, minmax(0, 1fr));
      gap: 1.5rem;
    }

    .training-card {
      grid-column: span 6;
      padding: 1rem;
      border-radius: 24px;
    }

    .full-width {
      grid-column: span 12;
    }

    .card-head {
      margin-bottom: 1rem;
    }

    .timeline {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
    }

    .timeline article,
    .achievement-row {
      padding: 1rem;
      border-radius: 20px;
      background: rgba(16, 42, 67, 0.05);
    }

    .achievement-row {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 0.8rem;
    }

    .achievement-row:last-child {
      margin-bottom: 0;
    }

    .achievement-row small,
    .timeline p,
    .achievement-row p,
    .goal-stack p {
      margin: 0.35rem 0 0;
      color: var(--ld-muted);
    }

    .goal-stack {
      display: grid;
      gap: 1rem;
    }

    @media (max-width: 992px) {
      .training-card,
      .full-width {
        grid-column: span 12;
      }
    }
  `]
})
export class TrainingPageComponent {
  private readonly platformService = inject(PlatformService);

  protected readonly trainingPlan = this.platformService.trainingPlan;
  protected readonly trainingExercises = this.platformService.trainingExercises;
  protected readonly achievements = this.platformService.achievements;
  protected readonly weeklyProgress = this.platformService.weeklyTrainingProgress;
  protected readonly completedExercises = computed(() => this.trainingExercises().filter((exercise) => exercise.unlocked).length);
}