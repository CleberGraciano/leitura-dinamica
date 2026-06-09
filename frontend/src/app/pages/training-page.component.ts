import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';

import { PlatformService, TrainingExercise } from '../data/platform.service';

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

      <section class="card-surface training-card full-width">
        <header class="card-head">
          <h2 class="section-title">Execucao do exercicio</h2>
        </header>

        @if (activeExercise(); as exercise) {
          <div class="runner-card">
            <div>
              <strong>{{ exercise.name }}</strong>
              <p>{{ exercise.description }}</p>
              <small>Meta: {{ exercise.targetWpm }} WPM</small>
            </div>

            <label class="field">
              <span>WPM atingido</span>
              <input type="range" min="100" max="1200" step="10" [value]="exerciseWpm()" (input)="exerciseWpm.set(+$any($event.target).value)" />
              <strong>{{ exerciseWpm() }} WPM</strong>
            </label>

            <div class="goal-stack">
              <strong>{{ exerciseSeconds() }}s cronometrados</strong>
              <div class="progress-ui"><span [style.width.%]="exerciseCompletionPercent()"></span></div>
              <p>{{ exerciseSeconds() >= minimumExerciseSeconds ? 'Tempo minimo atingido.' : 'Mantenha o treino por pelo menos ' + minimumExerciseSeconds + ' segundos.' }}</p>
            </div>

            <div class="action-row">
              <button class="btn-ld-secondary" type="button" (click)="savePartial()" [disabled]="submitting()">Salvar parcial</button>
              <button class="btn-ld" type="button" (click)="finishExercise()" [disabled]="submitting() || exerciseSeconds() < minimumExerciseSeconds">Concluir exercicio</button>
            </div>
          </div>
        } @else {
          <div class="runner-card idle-card">
            <strong>Nenhum exercicio em execucao</strong>
            <p>Escolha um exercicio liberado abaixo para iniciar um treino com cronometro e meta de WPM.</p>
          </div>
        }
      </section>

      <section class="card-surface training-card">
        <header class="card-head">
          <h2 class="section-title">Exercicios</h2>
        </header>
        @for (exercise of trainingExercises(); track exercise.id) {
          <div class="achievement-row">
            <div>
              <strong>{{ exercise.name }}</strong>
              <p>{{ exercise.description }}</p>
              <small>{{ exercise.helper }}</small>
            </div>
            <div class="exercise-side">
              <span class="status-pill" [class.success]="exercise.unlocked" [class.muted]="!exercise.unlocked">{{ exercise.unlocked ? 'Liberado' : 'Bloqueado' }}</span>
              <small>{{ exercise.completedCount }} concluidos</small>
              <button class="btn-ld-secondary" type="button" (click)="startExercise(exercise)" [disabled]="!exercise.unlocked || activeExerciseKey() === exercise.key">{{ activeExerciseKey() === exercise.key ? 'Em execucao' : 'Iniciar' }}</button>
            </div>
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
          <p>{{ weeklyProgress() }}% da meta semanal concluida com base nas sessoes persistidas de treino.</p>
        </div>

        <div class="session-grid">
          @for (session of recentSessions(); track session.id) {
            <article>
              <strong>{{ session.exerciseName }}</strong>
              <p>{{ session.achievedWpm }} WPM • {{ session.durationSeconds }}s</p>
              <small>{{ session.completed ? 'Concluido' : 'Parcial' }}</small>
            </article>
          }
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

    .timeline,
    .session-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
    }

    .timeline article,
    .session-grid article,
    .achievement-row,
    .runner-card {
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

    .exercise-side {
      display: grid;
      gap: 0.5rem;
      justify-items: end;
      text-align: right;
    }

    .runner-card {
      display: grid;
      gap: 1rem;
    }

    .idle-card {
      text-align: center;
    }

    .field {
      display: grid;
      gap: 0.45rem;
    }

    .goal-stack {
      display: grid;
      gap: 1rem;
    }

    .action-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
    }

    .achievement-row small,
    .timeline p,
    .session-grid p,
    .achievement-row p,
    .goal-stack p,
    .runner-card p {
      margin: 0.35rem 0 0;
      color: var(--ld-muted);
    }

    @media (max-width: 992px) {
      .training-card,
      .full-width {
        grid-column: span 12;
      }

      .achievement-row {
        flex-direction: column;
      }

      .exercise-side {
        justify-items: start;
        text-align: left;
      }
    }
  `]
})
export class TrainingPageComponent implements OnDestroy {
  private readonly platformService = inject(PlatformService);
  private timer?: Subscription;

  protected readonly minimumExerciseSeconds = 30;
  protected readonly trainingPlan = this.platformService.trainingPlan;
  protected readonly trainingExercises = this.platformService.trainingExercises;
  protected readonly achievements = this.platformService.achievements;
  protected readonly weeklyProgress = this.platformService.weeklyTrainingProgress;
  protected readonly recentSessions = this.platformService.recentTrainingSessions;
  protected readonly activeExerciseKey = signal<string | null>(null);
  protected readonly exerciseSeconds = signal(0);
  protected readonly exerciseWpm = signal(200);
  protected readonly submitting = signal(false);
  protected readonly activeExercise = computed(() => this.trainingExercises().find((exercise) => exercise.key === this.activeExerciseKey()) ?? null);
  protected readonly completedExercises = computed(() => this.trainingExercises().filter((exercise) => exercise.unlocked).length);
  protected readonly exerciseCompletionPercent = computed(() => Math.min(100, Math.round((this.exerciseSeconds() / this.minimumExerciseSeconds) * 100)));

  ngOnDestroy(): void {
    this.timer?.unsubscribe();
  }

  startExercise(exercise: TrainingExercise): void {
    if (!exercise.unlocked) {
      return;
    }

    this.timer?.unsubscribe();
    this.activeExerciseKey.set(exercise.key);
    this.exerciseSeconds.set(0);
    this.exerciseWpm.set(exercise.targetWpm);
    this.timer = interval(1000).subscribe(() => this.exerciseSeconds.update((value) => value + 1));
  }

  savePartial(): void {
    this.persistExercise(false);
  }

  finishExercise(): void {
    this.persistExercise(true);
  }

  private persistExercise(completed: boolean): void {
    const activeExercise = this.activeExercise();
    if (!activeExercise) {
      return;
    }

    this.submitting.set(true);
    this.platformService.executeTrainingExercise({
      exerciseKey: activeExercise.key,
      achievedWpm: this.exerciseWpm(),
      durationSeconds: Math.max(1, this.exerciseSeconds()),
      completed
    }).subscribe({
      next: () => {
        this.submitting.set(false);
        this.timer?.unsubscribe();
        this.activeExerciseKey.set(null);
        this.exerciseSeconds.set(0);
      },
      error: () => {
        this.submitting.set(false);
      }
    });
  }
}