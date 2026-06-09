import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { Subscription, interval } from 'rxjs';

import { PlatformService, TrainingExercise } from '../data/platform.service';

type TrainingReaderMode = 1 | 2 | 3;

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
            <div class="runner-layout">
              <div class="runner-config">
                <div>
                  <strong>{{ exercise.name }}</strong>
                  <p>{{ exercise.description }}</p>
                  <small>Meta: {{ exercise.targetWpm }} WPM</small>
                </div>

                <label class="field">
                  <span>Livro base do treino</span>
                  <select [value]="selectedBookId()" (change)="setTrainingBook($any($event.target).value)">
                    @for (book of trainingBooks(); track book.id) {
                      <option [value]="book.id">{{ book.title }}</option>
                    }
                  </select>
                </label>

                <label class="field">
                  <span>WPM atingido</span>
                  <input type="range" min="100" max="1200" step="10" [value]="exerciseWpm()" (input)="setExerciseWpm($any($event.target).value)" />
                  <strong>{{ exerciseWpm() }} WPM</strong>
                </label>

                <div class="field">
                  <span>Modo de exibicao</span>
                  <div class="mode-switch three-up">
                    @for (mode of modes; track mode.value) {
                      <button type="button" class="mode-button" [class.active]="chunkSize() === mode.value" (click)="setChunkSize(mode.value)">{{ mode.label }}</button>
                    }
                  </div>
                </div>

                <label class="toggle-row">
                  <input type="checkbox" [checked]="orpEnabled()" (change)="orpEnabled.set($any($event.target).checked)">
                  <span>ORP ativado</span>
                </label>

                <div class="goal-stack compact-goal">
                  <strong>{{ exerciseSeconds() }}s cronometrados</strong>
                  <div class="progress-ui"><span [style.width.%]="exerciseCompletionPercent()"></span></div>
                  <p>{{ exerciseSeconds() >= minimumExerciseSeconds ? 'Tempo minimo atingido.' : 'Mantenha o treino por pelo menos ' + minimumExerciseSeconds + ' segundos.' }}</p>
                </div>
              </div>

              @if (hasRenderableContent()) {
                <div class="runner-stage-wrap">
                  <div class="word-stage" [class.large-mode]="chunkSize() === 1">
                    @for (part of highlightedWords(); track $index) {
                      <span class="orp-word"><span>{{ part.start }}</span><strong>{{ part.focus }}</strong><span>{{ part.end }}</span></span>
                    }
                  </div>

                  <div class="stats-row">
                    <span class="status-pill muted">Livro {{ selectedBook()?.title }}</span>
                    <span class="status-pill muted">Palavra {{ displayWordIndex() }} / {{ totalWords() }}</span>
                    <span class="status-pill success">{{ readingProgress() }}% do texto-base</span>
                  </div>
                </div>
              } @else {
                <div class="empty-stage">
                  <strong>Sem texto para exibir no treino</strong>
                  <p>Cadastre um livro com conteudo extraido ou texto manual para usar os exercicios com palavras na tela.</p>
                </div>
              }
            </div>

            <div class="action-row">
              <button class="btn-ld-secondary" type="button" (click)="savePartial()" [disabled]="submitting() || !hasRenderableContent()">Salvar parcial</button>
              <button class="btn-ld" type="button" (click)="finishExercise()" [disabled]="submitting() || exerciseSeconds() < minimumExerciseSeconds || !hasRenderableContent()">Concluir exercicio</button>
            </div>
          </div>
        } @else {
          <div class="runner-card idle-card">
            <strong>Nenhum exercicio em execucao</strong>
            <p>Escolha um exercicio liberado abaixo para iniciar um treino com cronometro, meta de WPM e exibicao de palavras reais.</p>
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
              <button class="btn-ld-secondary" type="button" (click)="startExercise(exercise)" [disabled]="!exercise.unlocked || activeExerciseKey() === exercise.key || !trainingBooks().length">{{ activeExerciseKey() === exercise.key ? 'Em execucao' : 'Iniciar' }}</button>
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

    .runner-layout {
      display: grid;
      grid-template-columns: minmax(280px, 360px) minmax(0, 1fr);
      gap: 1rem;
      align-items: start;
    }

    .runner-config {
      display: grid;
      gap: 1rem;
    }

    .runner-stage-wrap {
      display: grid;
      gap: 1rem;
    }

    .idle-card {
      text-align: center;
    }

    .empty-stage {
      display: grid;
      gap: 0.5rem;
      min-height: 280px;
      align-content: center;
      padding: 2rem;
      border-radius: 24px;
      background: rgba(16, 42, 67, 0.05);
      text-align: center;
    }

    .field {
      display: grid;
      gap: 0.45rem;
    }

    .mode-switch {
      display: grid;
      gap: 0.5rem;
      padding: 0.35rem;
      border-radius: 16px;
      background: rgba(16, 42, 67, 0.06);
    }

    .three-up {
      grid-template-columns: repeat(3, 1fr);
    }

    .mode-button {
      padding: 0.75rem;
      border-radius: 12px;
      font-weight: 700;
      cursor: pointer;
      color: var(--ld-muted);
    }

    .mode-button.active {
      background: white;
      color: var(--ld-ink);
      box-shadow: 0 8px 18px rgba(16, 42, 67, 0.08);
    }

    .toggle-row {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      font-weight: 600;
    }

    .goal-stack {
      display: grid;
      gap: 1rem;
    }

    .compact-goal {
      gap: 0.75rem;
    }

    .word-stage {
      min-height: 280px;
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: center;
      gap: 1rem;
      padding: 2rem;
      border-radius: 24px;
      background: linear-gradient(180deg, rgba(16, 42, 67, 0.96), rgba(16, 42, 67, 0.84));
      color: white;
      font-size: clamp(1.8rem, 5vw, 3.5rem);
      text-align: center;
    }

    .large-mode {
      font-size: clamp(2.6rem, 6vw, 4.6rem);
    }

    .orp-word strong {
      color: #ffb703;
    }

    .stats-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      justify-content: center;
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

      .runner-layout {
        grid-template-columns: 1fr;
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
  private playbackTimer?: Subscription;

  protected readonly minimumExerciseSeconds = 30;
  protected readonly trainingPlan = this.platformService.trainingPlan;
  protected readonly trainingExercises = this.platformService.trainingExercises;
  protected readonly achievements = this.platformService.achievements;
  protected readonly weeklyProgress = this.platformService.weeklyTrainingProgress;
  protected readonly recentSessions = this.platformService.recentTrainingSessions;
  protected readonly trainingBooks = computed(() => this.platformService.activeBooks().filter((book) => book.content.trim().length > 0));
  protected readonly activeExerciseKey = signal<string | null>(null);
  protected readonly selectedBookId = signal(0);
  protected readonly chunkSize = signal<TrainingReaderMode>(1);
  protected readonly orpEnabled = signal(true);
  protected readonly currentIndex = signal(0);
  protected readonly exerciseSeconds = signal(0);
  protected readonly exerciseWpm = signal(200);
  protected readonly submitting = signal(false);
  protected readonly modes = [
    { value: 1 as TrainingReaderMode, label: '1 palavra' },
    { value: 2 as TrainingReaderMode, label: '2 palavras' },
    { value: 3 as TrainingReaderMode, label: '3 palavras' }
  ];
  protected readonly activeExercise = computed(() => this.trainingExercises().find((exercise) => exercise.key === this.activeExerciseKey()) ?? null);
  protected readonly selectedBook = computed(() => this.trainingBooks().find((book) => book.id === this.selectedBookId()) ?? this.trainingBooks()[0] ?? null);
  protected readonly words = computed(() => this.selectedBook()?.content.trim().split(/\s+/).filter(Boolean) ?? []);
  protected readonly totalWords = computed(() => this.words().length);
  protected readonly highlightedWords = computed(() => this.words().slice(this.currentIndex(), this.currentIndex() + this.chunkSize()).map((word) => this.decorateWord(word)));
  protected readonly displayWordIndex = computed(() => Math.min(this.currentIndex() + 1, Math.max(this.totalWords(), 1)));
  protected readonly readingProgress = computed(() => this.totalWords() === 0 ? 0 : Math.min(100, Math.round(((this.currentIndex() + this.chunkSize()) / this.totalWords()) * 100)));
  protected readonly completedExercises = computed(() => this.trainingExercises().filter((exercise) => exercise.unlocked).length);
  protected readonly exerciseCompletionPercent = computed(() => Math.min(100, Math.round((this.exerciseSeconds() / this.minimumExerciseSeconds) * 100)));
  protected readonly hasRenderableContent = computed(() => this.totalWords() > 0);

  constructor() {
    effect(() => {
      const books = this.trainingBooks();
      if (books.length === 0) {
        this.selectedBookId.set(0);
        this.currentIndex.set(0);
        return;
      }

      const selected = books.find((book) => book.id === this.selectedBookId()) ?? books[0];
      if (selected.id !== this.selectedBookId()) {
        this.selectedBookId.set(selected.id);
      }
    });
  }

  ngOnDestroy(): void {
    this.stopExerciseTimers();
  }

  startExercise(exercise: TrainingExercise): void {
    if (!exercise.unlocked || !this.hasRenderableContent()) {
      return;
    }

    this.stopExerciseTimers();
    this.activeExerciseKey.set(exercise.key);
    this.currentIndex.set(0);
    this.exerciseSeconds.set(0);
    this.exerciseWpm.set(exercise.targetWpm);
    this.restartPlaybackTimer();
    this.timer = interval(1000).subscribe(() => this.exerciseSeconds.update((value) => value + 1));
  }

  setTrainingBook(bookId: number | string): void {
    this.selectedBookId.set(Number(bookId));
    this.currentIndex.set(0);
  }

  setExerciseWpm(value: number | string): void {
    this.exerciseWpm.set(Number(value));
    if (this.activeExercise()) {
      this.restartPlaybackTimer();
    }
  }

  setChunkSize(value: TrainingReaderMode | string): void {
    this.chunkSize.set(Number(value) as TrainingReaderMode);
    if (this.activeExercise()) {
      this.restartPlaybackTimer();
    }
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
        this.stopExerciseTimers();
        this.activeExerciseKey.set(null);
        this.currentIndex.set(0);
        this.exerciseSeconds.set(0);
      },
      error: () => {
        this.submitting.set(false);
      }
    });
  }

  private restartPlaybackTimer(): void {
    this.playbackTimer?.unsubscribe();
    if (!this.hasRenderableContent()) {
      return;
    }

    const delay = Math.max(60, Math.round((60000 / Math.max(this.exerciseWpm(), 1)) * this.chunkSize()));
    this.playbackTimer = interval(delay).subscribe(() => {
      if (this.totalWords() === 0) {
        return;
      }

      const nextIndex = this.currentIndex() + this.chunkSize();
      this.currentIndex.set(nextIndex >= this.totalWords() ? 0 : nextIndex);
    });
  }

  private stopExerciseTimers(): void {
    this.timer?.unsubscribe();
    this.playbackTimer?.unsubscribe();
  }

  private decorateWord(word: string): { start: string; focus: string; end: string } {
    if (!this.orpEnabled()) {
      return { start: '', focus: word, end: '' };
    }

    const center = Math.floor(word.length / 2);
    return {
      start: word.slice(0, center),
      focus: word.charAt(center),
      end: word.slice(center + 1)
    };
  }
}