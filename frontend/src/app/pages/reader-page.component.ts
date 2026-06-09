import { DecimalPipe } from '@angular/common';
import { Component, OnDestroy, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription, interval } from 'rxjs';

import { PlatformService } from '../data/platform.service';

type ReaderMode = 1 | 2 | 3 | 4;

@Component({
  selector: 'app-reader-page',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  template: `
    <section class="reader-grid">
      <section class="card-surface config-card">
        <header class="card-head">
          <h2 class="section-title">Leitor dinamico</h2>
        </header>
        <div class="config-stack">
          <div class="field">
            <label for="book-select">Livro</label>
            <select id="book-select" [value]="selectedBookId()" (change)="changeBook($any($event.target).value)">
              @for (book of books(); track book.id) {
                <option [value]="book.id">{{ book.title }}</option>
              }
            </select>
          </div>

          <div class="field">
            <label for="speed-range">Velocidade: <strong>{{ wpm() }} WPM</strong></label>
            <input id="speed-range" type="range" min="100" max="1000" step="50" [value]="wpm()" (input)="setWpm($any($event.target).value)" />
          </div>

          <div class="field">
            <label>Modo de exibicao</label>
            <div class="mode-switch four-up">
              @for (mode of modes; track mode.value) {
                <button type="button" class="mode-button" [class.active]="chunkSize() === mode.value" (click)="setChunkSize(mode.value)">{{ mode.label }}</button>
              }
            </div>
          </div>

          <label class="toggle-row">
            <input type="checkbox" [checked]="orpEnabled()" (change)="orpEnabled.set($any($event.target).checked)">
            <span>ORP ativado</span>
          </label>
        </div>
      </section>

      <section class="card-surface player-card">
        <header class="card-head">
          <h2 class="section-title">{{ selectedBook()?.title || 'Selecione um livro' }}</h2>
        </header>
        <div class="player-area">
          <div class="word-stage" [class.large-mode]="chunkSize() === 1">
            @for (part of highlightedWords(); track $index) {
              <span class="orp-word"><span>{{ part.start }}</span><strong>{{ part.focus }}</strong><span>{{ part.end }}</span></span>
            }
          </div>

          <div class="player-controls">
            <button class="btn-ld-secondary" type="button" (click)="step(-chunkSize())">Anterior</button>
            <button class="btn-ld" type="button" (click)="togglePlayback()">{{ isRunning() ? 'Pausar' : 'Iniciar' }}</button>
            <button class="btn-ld-secondary" type="button" (click)="step(chunkSize())">Proxima</button>
            <button class="btn-ld-ghost" type="button" (click)="reset()">Reiniciar</button>
          </div>

          <div class="progress-ui"><span [style.width.%]="progress()"></span></div>

          <div class="stats-row">
            <span class="status-pill muted">Palavra {{ displayWordIndex() }} / {{ totalWords() }}</span>
            <span class="status-pill success">{{ progress() }}% lido</span>
            <span class="status-pill warning">Sessao {{ elapsedSeconds() | number:'1.0-0' }}s</span>
            <span class="status-pill muted">Restante {{ remainingSeconds() | number:'1.0-0' }}s</span>
          </div>
        </div>
      </section>

      <section class="card-surface side-card">
        <header class="card-head">
          <h2 class="section-title">Resumo da sessao</h2>
        </header>
        <div class="config-stack">
          <div><strong>{{ selectedBook()?.category }}</strong><p>{{ selectedBook()?.description }}</p></div>
          <div><strong>Ultima velocidade</strong><p>{{ selectedBook()?.lastWpm }} WPM</p></div>
          <div><strong>Retomar leitura</strong><p>Posicao salva: palavra {{ resumePosition() + 1 }}.</p></div>
          <a class="btn-ld-secondary" routerLink="/app/library">Voltar para biblioteca</a>
        </div>
      </section>
    </section>
  `,
  styles: [`
    .reader-grid {
      display: grid;
      grid-template-columns: 320px minmax(0, 1fr) 280px;
      gap: 1.5rem;
      align-items: start;
    }

    .config-card,
    .player-card,
    .side-card {
      padding: 1rem;
      border-radius: 24px;
    }

    .card-head {
      margin-bottom: 1rem;
    }

    .config-stack {
      display: grid;
      gap: 1rem;
    }

    .config-stack p {
      margin: 0.3rem 0 0;
      color: var(--ld-muted);
    }

    .mode-switch {
      display: grid;
      gap: 0.5rem;
      padding: 0.35rem;
      border-radius: 16px;
      background: rgba(16, 42, 67, 0.06);
    }

    .four-up {
      grid-template-columns: repeat(2, 1fr);
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

    .player-area {
      display: grid;
      gap: 1.25rem;
    }

    .player-controls,
    .stats-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      justify-content: center;
    }

    @media (max-width: 1200px) {
      .reader-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class ReaderPageComponent implements OnDestroy {
  private readonly platformService = inject(PlatformService);
  private playbackTimer?: Subscription;
  private secondTimer?: Subscription;

  protected readonly books = this.platformService.activeBooks;
  protected readonly selectedBookId = signal(0);
  protected readonly wpm = signal(300);
  protected readonly chunkSize = signal<ReaderMode>(1);
  protected readonly orpEnabled = signal(true);
  protected readonly currentIndex = signal(0);
  protected readonly isRunning = signal(false);
  protected readonly elapsedSeconds = signal(0);
  protected readonly modes = [
    { value: 1 as ReaderMode, label: '1 palavra' },
    { value: 2 as ReaderMode, label: '2 palavras' },
    { value: 3 as ReaderMode, label: '3 palavras' },
    { value: 4 as ReaderMode, label: 'Frase' }
  ];

  protected readonly selectedBook = computed(() => this.platformService.getBook(this.selectedBookId()) ?? this.books()[0]);
  protected readonly words = computed(() => this.selectedBook()?.content.trim().split(/\s+/) ?? []);
  protected readonly totalWords = computed(() => this.words().length);
  protected readonly progress = computed(() => this.totalWords() === 0 ? 0 : Math.round((this.currentIndex() / this.totalWords()) * 100));
  protected readonly remainingSeconds = computed(() => {
    const remainingWords = Math.max(this.totalWords() - this.currentIndex(), 0);
    return (remainingWords / Math.max(this.wpm(), 1)) * 60;
  });
  protected readonly highlightedWords = computed(() => {
    const chunk = this.words().slice(this.currentIndex(), this.currentIndex() + this.chunkSize());
    return chunk.map((word) => this.decorateWord(word));
  });
  protected readonly resumePosition = computed(() => this.selectedBookId() ? this.platformService.getResumeWordPosition(this.selectedBookId()) : 0);
  protected readonly displayWordIndex = computed(() => Math.min(this.currentIndex() + 1, Math.max(this.totalWords(), 1)));

  constructor() {
    effect(() => {
      const availableBooks = this.books();
      if (availableBooks.length === 0) {
        this.stopTimers();
        this.selectedBookId.set(0);
        this.currentIndex.set(0);
        this.elapsedSeconds.set(0);
        return;
      }

      const selectedId = this.selectedBookId();
      const selectedBook = availableBooks.find((book) => book.id === selectedId) ?? availableBooks[0];
      if (selectedBook.id !== selectedId) {
        this.selectedBookId.set(selectedBook.id);
      }

      if (!this.isRunning()) {
        const resumePosition = this.platformService.getResumeWordPosition(selectedBook.id);
        this.currentIndex.set(Math.min(resumePosition, Math.max(selectedBook.content.trim().split(/\s+/).length - 1, 0)));
        if (selectedBook.lastWpm > 0) {
          this.wpm.set(selectedBook.lastWpm);
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.stopTimers();
  }

  changeBook(bookId: number | string): void {
    this.pauseSession();
    const nextBookId = Number(bookId);
    this.selectedBookId.set(nextBookId);
    this.currentIndex.set(this.platformService.getResumeWordPosition(nextBookId));
    this.elapsedSeconds.set(0);
  }

  setWpm(value: number | string): void {
    this.wpm.set(Number(value));
    if (this.isRunning()) {
      this.restartPlaybackTimer();
    }
  }

  setChunkSize(value: ReaderMode | string): void {
    this.chunkSize.set(Number(value) as ReaderMode);
    if (this.isRunning()) {
      this.restartPlaybackTimer();
    }
  }

  togglePlayback(): void {
    if (this.isRunning()) {
      this.pauseSession();
      return;
    }

    this.startSession();
  }

  step(delta: number): void {
    if (this.isRunning()) {
      this.pauseSession();
    }

    const nextIndex = Math.min(Math.max(this.currentIndex() + delta, 0), Math.max(this.totalWords() - 1, 0));
    this.currentIndex.set(nextIndex);
  }

  reset(): void {
    this.pauseSession();
    this.currentIndex.set(0);
    this.elapsedSeconds.set(0);
    if (this.selectedBookId()) {
      this.platformService.pauseReadingSession(this.selectedBookId(), 0, this.wpm(), this.totalWords(), 0).subscribe();
    }
  }

  private startSession(): void {
    if (!this.selectedBook()) {
      return;
    }

    this.isRunning.set(true);
    this.platformService.startReadingSession(this.selectedBookId(), this.currentIndex(), this.wpm(), this.totalWords()).subscribe();
    this.restartPlaybackTimer();
    this.secondTimer?.unsubscribe();
    this.secondTimer = interval(1000).subscribe(() => this.elapsedSeconds.update((value) => value + 1));
  }

  private pauseSession(): void {
    if (!this.selectedBookId()) {
      this.stopTimers();
      return;
    }

    const wasRunning = this.isRunning();
    this.stopTimers();
    if (!wasRunning && this.elapsedSeconds() === 0) {
      return;
    }

    this.platformService.pauseReadingSession(this.selectedBookId(), this.currentIndex(), this.wpm(), this.totalWords(), this.elapsedSeconds()).subscribe();
  }

  private finishSession(): void {
    if (!this.selectedBookId()) {
      this.stopTimers();
      return;
    }

    this.stopTimers();
    this.platformService.finishReadingSession(this.selectedBookId(), this.currentIndex(), this.wpm(), this.totalWords(), this.elapsedSeconds()).subscribe();
  }

  private restartPlaybackTimer(): void {
    this.playbackTimer?.unsubscribe();
    const delay = Math.max(60, Math.round((60000 / Math.max(this.wpm(), 1)) * this.chunkSize()));
    this.playbackTimer = interval(delay).subscribe(() => {
      const nextIndex = this.currentIndex() + this.chunkSize();
      if (nextIndex >= this.totalWords()) {
        this.currentIndex.set(Math.max(this.totalWords() - 1, 0));
        this.finishSession();
        return;
      }

      this.currentIndex.set(nextIndex);
    });
  }

  private stopTimers(): void {
    this.isRunning.set(false);
    this.playbackTimer?.unsubscribe();
    this.secondTimer?.unsubscribe();
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