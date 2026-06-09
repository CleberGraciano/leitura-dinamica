import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { forkJoin, map, Observable, tap } from 'rxjs';

import { API_BASE_URL } from '../core/api.config';
import { AuthSessionService, AuthUser } from '../core/auth-session.service';

export interface DashboardCard {
  label: string;
  value: string;
  helper: string;
}

export interface WeeklySeriesPoint {
  label: string;
  value: number;
}

export interface BookSummary {
  id: number;
  title: string;
  author: string;
  categoryId: number | null;
  category: string;
  progress: number;
  favorite: boolean;
  publicBook: boolean;
  archived: boolean;
  fileType: 'PDF' | 'EPUB' | 'TXT' | 'TEXT';
  description: string;
  content: string;
  lastWpm: number;
  filePath?: string | null;
}

export interface BookUpsertInput {
  title: string;
  author: string;
  categoryId: number | null;
  fileType: BookSummary['fileType'];
  description: string;
  content: string;
  favorite: boolean;
  publicBook: boolean;
  archived?: boolean;
}

export interface CategorySummary {
  id: number;
  name: string;
  description: string;
  favorite: boolean;
}

export type ReadingSessionStatus = 'STARTED' | 'PAUSED' | 'FINISHED';

export interface ReadingHistoryEntry {
  id: number;
  bookId: number;
  bookTitle: string;
  createdAt: string;
  progress: number;
  wordsRead: number;
  durationSeconds: number;
  wpm: number;
  status: ReadingSessionStatus;
  lastWordPosition: number;
}

export interface TrainingWeek {
  week: number;
  stage: 'Iniciante' | 'Intermediario' | 'Avancado';
  targetWpm: number;
  focus: string;
}

export interface TrainingExercise {
  id: number;
  key: string;
  name: string;
  description: string;
  helper: string;
  targetWpm: number;
  unlocked: boolean;
  completedCount: number;
  lastCompletedAt: string | null;
}

export interface TrainingSessionSummary {
  id: number;
  exerciseKey: string;
  exerciseName: string;
  targetWpm: number;
  achievedWpm: number;
  durationSeconds: number;
  completed: boolean;
  createdAt: string;
}

export interface AchievementSummary {
  id: number;
  name: string;
  description: string;
  unlocked: boolean;
  achievedAt: string | null;
}

export interface AdminMetric {
  label: string;
  value: string;
  helper: string;
}

export interface UserSummary extends AuthUser {
}

interface BookApiResponse {
  id: number;
  title: string;
  author: string | null;
  description: string | null;
  cover: string | null;
  filePath: string | null;
  fileType: BookSummary['fileType'];
  categoryId: number | null;
  categoryName: string | null;
  userId: number;
  createdAt: string;
  favorite: boolean;
  archived: boolean;
  publicBook: boolean;
  wordCount: number;
  contentText: string | null;
}

interface CategoryApiResponse {
  id: number;
  name: string;
  description: string | null;
}

interface ReadingHistoryResponse {
  items: ReadingHistoryEntry[];
}

interface ReadingCommand {
  bookId: number;
  wpm: number;
  wordsRead: number;
  durationSeconds: number;
  progress: number;
  wordPosition: number;
}

interface TrainingExerciseApiResponse {
  id: number;
  key: string;
  name: string;
  description: string;
  helper: string;
  targetWpm: number;
  unlocked: boolean;
  completedCount: number;
  lastCompletedAt: string | null;
}

interface TrainingSessionApiResponse {
  id: number;
  exerciseKey: string;
  exerciseName: string;
  targetWpm: number;
  achievedWpm: number;
  durationSeconds: number;
  completed: boolean;
  createdAt: string;
}

interface TrainingOverviewResponse {
  exercises: TrainingExerciseApiResponse[];
  recentSessions: TrainingSessionApiResponse[];
  weeklyProgress: number;
}

interface TrainingExecutionRequest {
  exerciseKey: string;
  achievedWpm: number;
  durationSeconds: number;
  completed: boolean;
}

interface AchievementApiResponse {
  id: number;
  name: string;
  description: string;
  badge: string | null;
  unlocked: boolean;
  achievedAt: string | null;
}

interface AchievementResponse {
  items: AchievementApiResponse[];
}

@Injectable({ providedIn: 'root' })
export class PlatformService {
  private readonly http = inject(HttpClient);
  private readonly authSession = inject(AuthSessionService);

  private readonly booksState = signal<BookSummary[]>([]);
  private readonly categoriesState = signal<CategorySummary[]>([]);
  private readonly historyState = signal<ReadingHistoryEntry[]>([]);
  private readonly usersState = signal<UserSummary[]>([]);
  private readonly trainingExercisesState = signal<TrainingExercise[]>([]);
  private readonly trainingSessionsState = signal<TrainingSessionSummary[]>([]);
  private readonly achievementsState = signal<AchievementSummary[]>([]);
  private readonly weeklyTrainingProgressState = signal(0);

  private readonly trainingState = signal<TrainingWeek[]>([
    { week: 1, stage: 'Iniciante', targetWpm: 200, focus: 'Adaptacao ao fluxo palavra unica' },
    { week: 2, stage: 'Iniciante', targetWpm: 250, focus: 'Reducao de subvocalizacao' },
    { week: 3, stage: 'Iniciante', targetWpm: 300, focus: 'Ajuste de foco ORP' },
    { week: 4, stage: 'Iniciante', targetWpm: 350, focus: 'Consistencia com textos tecnicos' },
    { week: 5, stage: 'Intermediario', targetWpm: 400, focus: 'Agrupamento de palavras' },
    { week: 6, stage: 'Intermediario', targetWpm: 450, focus: 'Retencao de conceitos-chave' },
    { week: 7, stage: 'Intermediario', targetWpm: 500, focus: 'Sessao longa e estabilidade' },
    { week: 8, stage: 'Intermediario', targetWpm: 550, focus: 'Leitura com contexto academico' },
    { week: 9, stage: 'Avancado', targetWpm: 600, focus: 'Alternancia entre temas complexos' },
    { week: 10, stage: 'Avancado', targetWpm: 700, focus: 'Ritmo alto com pausas controladas' },
    { week: 11, stage: 'Avancado', targetWpm: 800, focus: 'Compreensao sob alta velocidade' },
    { week: 12, stage: 'Avancado', targetWpm: 1000, focus: 'Manutencao de performance premium' }
  ]);

  readonly currentUser = this.authSession.currentUser;
  readonly books = computed(() => this.booksState());
  readonly categories = computed(() => this.categoriesState());
  readonly history = computed(() => this.historyState());
  readonly users = computed(() => this.usersState());
  readonly trainingPlan = computed(() => this.trainingState());
  readonly trainingExercises = computed(() => this.trainingExercisesState());
  readonly recentTrainingSessions = computed(() => this.trainingSessionsState());
  readonly achievements = computed(() => this.achievementsState());
  readonly weeklyTrainingProgress = computed(() => this.weeklyTrainingProgressState());
  readonly favoriteBooks = computed(() => this.booksState().filter((book) => book.favorite));
  readonly activeBooks = computed(() => this.booksState().filter((book) => !book.archived));
  readonly canAccessAdmin = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'ROLE_ADMIN' || role === 'ROLE_EDITOR';
  });
  readonly canManageUsers = computed(() => this.currentUser()?.role === 'ROLE_ADMIN');

  readonly cards = computed<DashboardCard[]>(() => {
    const books = this.booksState();
    const currentUser = this.currentUser();
    const favorites = books.filter((book) => book.favorite).length;
    const publicBooks = books.filter((book) => book.publicBook).length;
    const accessLabel = currentUser ? this.formatRole(currentUser.role) : 'Visitante';
    const planLabel = currentUser ? this.formatPlan(currentUser.planType) : 'Sem plano';

    return [
      { label: 'Livros cadastrados', value: String(books.length), helper: 'Persistidos no backend por usuario' },
      { label: 'Favoritos', value: String(favorites), helper: 'Marcados na sua biblioteca real' },
      { label: 'Livros publicos', value: String(publicBooks), helper: 'Disponiveis para catalogo compartilhado' },
      { label: 'Seu acesso', value: accessLabel, helper: planLabel }
    ];
  });

  readonly weeklyReadings = computed<WeeklySeriesPoint[]>(() => {
    const grouped = new Map<string, number>();
    for (const book of this.booksState()) {
      grouped.set(book.category, (grouped.get(book.category) ?? 0) + 1);
    }

    return [...grouped.entries()]
      .sort((left, right) => right[1] - left[1])
      .slice(0, 6)
      .map(([label, value]) => ({ label, value }));
  });

  readonly weeklySpeed = computed<WeeklySeriesPoint[]>(() => {
    const types: BookSummary['fileType'][] = ['PDF', 'EPUB', 'TXT', 'TEXT'];
    return types.map((type) => ({
      label: type,
      value: this.historyState()
        .filter((entry) => this.getBook(entry.bookId)?.fileType === type)
        .reduce((total, entry) => total + entry.wordsRead, 0)
    })).filter((point) => point.value > 0);
  });

  readonly adminMetrics = computed<AdminMetric[]>(() => {
    const users = this.usersState();
    const books = this.booksState();
    const activeUsers = users.length > 0 ? users.filter((user) => user.active).length : (this.currentUser()?.active ? 1 : 0);
    const elevatedProfiles = users.filter((user) => user.role === 'ROLE_ADMIN' || user.role === 'ROLE_EDITOR').length;
    const premiumUsers = users.filter((user) => user.planType !== 'FREE').length;

    return [
      { label: 'Usuarios ativos', value: String(activeUsers), helper: users.length > 0 ? 'Carregados do backend administrativo' : 'Perfil autenticado atual' },
      { label: 'Livros publicos', value: String(books.filter((book) => book.publicBook).length), helper: 'Disponiveis no catalogo' },
      { label: 'Perfis elevados', value: String(elevatedProfiles), helper: 'Admins e editores cadastrados' },
      { label: 'Planos premium', value: String(premiumUsers), helper: 'Usuarios com acesso pago' }
    ];
  });

  constructor() {
    effect(() => {
      const accessToken = this.authSession.accessToken();
      if (!accessToken) {
        this.booksState.set([]);
        this.categoriesState.set([]);
        this.historyState.set([]);
        this.usersState.set([]);
        this.trainingExercisesState.set([]);
        this.trainingSessionsState.set([]);
        this.achievementsState.set([]);
        this.weeklyTrainingProgressState.set(0);
        return;
      }

      this.refreshAppData();
    });
  }

  refreshAppData(): void {
    this.loadCategories();
    this.loadBooks();
    this.loadReadingHistory();
    this.loadTrainingOverview();
    this.loadAchievements();
    if (this.canManageUsers()) {
      this.loadUsers();
      return;
    }
    this.usersState.set([]);
  }

  createBook(input: BookUpsertInput, file?: File | null): Observable<BookSummary> {
    return this.http.post<BookApiResponse>(`${API_BASE_URL}/books`, this.buildBookFormData(input, file ?? null)).pipe(
      map((response) => this.mapBookResponse(response)),
      tap((book) => {
        this.booksState.update((books) => [book, ...books.filter((currentBook) => currentBook.id !== book.id)]);
        this.loadBooks();
      })
    );
  }

  updateBook(bookId: number, input: BookUpsertInput, file?: File | null): Observable<BookSummary> {
    return this.http.put<BookApiResponse>(`${API_BASE_URL}/books/${bookId}`, this.buildBookFormData(input, file ?? null)).pipe(
      map((response) => this.mapBookResponse(response)),
      tap((book) => {
        this.booksState.update((books) => books.map((currentBook) => currentBook.id === book.id ? book : currentBook));
        this.historyState.update((history) => history.map((entry) => entry.bookId === book.id ? { ...entry, bookTitle: book.title } : entry));
        this.loadBooks();
      })
    );
  }

  deleteBook(bookId: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/books/${bookId}`).pipe(
      tap(() => {
        this.booksState.update((books) => books.filter((book) => book.id !== bookId));
        this.historyState.update((history) => history.filter((entry) => entry.bookId !== bookId));
        this.loadBooks();
      })
    );
  }

  toggleFavorite(bookId: number): void {
    const book = this.getBook(bookId);
    if (!book) {
      return;
    }

    this.updateBook(bookId, this.toBookUpsertInput(book, { favorite: !book.favorite })).subscribe();
  }

  toggleArchive(bookId: number): void {
    const book = this.getBook(bookId);
    if (!book) {
      return;
    }

    this.updateBook(bookId, this.toBookUpsertInput(book, { archived: !book.archived })).subscribe();
  }

  toggleCategoryFavorite(categoryId: number): void {
    this.categoriesState.update((categories) => categories.map((category) =>
      category.id === categoryId ? { ...category, favorite: !category.favorite } : category
    ));
  }

  getBook(bookId: number): BookSummary | undefined {
    return this.booksState().find((book) => book.id === bookId);
  }

  getResumeWordPosition(bookId: number): number {
    return this.historyState().find((entry) => entry.bookId === bookId)?.lastWordPosition ?? 0;
  }

  startReadingSession(bookId: number, wordPosition: number, wpm: number, totalWords: number): Observable<ReadingHistoryEntry> {
    return this.syncReading('start', this.buildReadingCommand(bookId, wordPosition, wpm, totalWords, 0));
  }

  pauseReadingSession(bookId: number, wordPosition: number, wpm: number, totalWords: number, durationSeconds: number): Observable<ReadingHistoryEntry> {
    return this.syncReading('pause', this.buildReadingCommand(bookId, wordPosition, wpm, totalWords, durationSeconds));
  }

  finishReadingSession(bookId: number, wordPosition: number, wpm: number, totalWords: number, durationSeconds: number): Observable<ReadingHistoryEntry> {
    return this.syncReading('finish', this.buildReadingCommand(bookId, wordPosition, wpm, totalWords, durationSeconds));
  }

  executeTrainingExercise(payload: TrainingExecutionRequest): Observable<TrainingSessionSummary> {
    return this.http.post<TrainingSessionApiResponse>(`${API_BASE_URL}/training/execute`, payload).pipe(
      map((response) => this.mapTrainingSession(response)),
      tap((session) => {
        this.trainingSessionsState.update((sessions) => [session, ...sessions].slice(0, 8));
        forkJoin([this.http.get<TrainingOverviewResponse>(`${API_BASE_URL}/training/overview`), this.http.get<AchievementResponse>(`${API_BASE_URL}/achievements`)]).subscribe({
          next: ([trainingOverview, achievements]) => {
            this.applyTrainingOverview(trainingOverview);
            this.achievementsState.set(achievements.items.map((item) => this.mapAchievement(item)));
          }
        });
      })
    );
  }

  private syncReading(action: 'start' | 'pause' | 'finish', payload: ReadingCommand): Observable<ReadingHistoryEntry> {
    return this.http.post<ReadingHistoryEntry>(`${API_BASE_URL}/reading/${action}`, payload).pipe(
      tap((entry) => this.applyReadingEntry(entry))
    );
  }

  private applyReadingEntry(entry: ReadingHistoryEntry): void {
    this.historyState.update((history) => [entry, ...history.filter((item) => item.id !== entry.id)].slice(0, 12));
    this.booksState.update((books) => books.map((book) => book.id === entry.bookId ? {
      ...book,
      progress: Math.round(entry.progress),
      lastWpm: entry.wpm
    } : book));
  }

  private buildReadingCommand(bookId: number, wordPosition: number, wpm: number, totalWords: number, durationSeconds: number): ReadingCommand {
    return {
      bookId,
      wpm,
      wordsRead: Math.max(wordPosition, 0),
      durationSeconds: Math.max(durationSeconds, 0),
      progress: totalWords === 0 ? 0 : Math.min(100, Math.round((wordPosition / Math.max(totalWords, 1)) * 100)),
      wordPosition: Math.max(wordPosition, 0)
    };
  }

  private loadCategories(): void {
    this.http.get<CategoryApiResponse[]>(`${API_BASE_URL}/categories`).pipe(
      map((categories) => categories.map((category) => ({
        id: category.id,
        name: category.name,
        description: category.description ?? '',
        favorite: this.categoriesState().find((currentCategory) => currentCategory.id === category.id)?.favorite ?? false
      })))
    ).subscribe({
      next: (categories) => this.categoriesState.set(categories),
      error: () => this.categoriesState.set([])
    });
  }

  private loadBooks(): void {
    this.http.get<BookApiResponse[]>(`${API_BASE_URL}/books`).pipe(
      map((books) => books.map((book) => this.mapBookResponse(book)))
    ).subscribe({
      next: (books) => {
        this.booksState.set(books.map((book) => {
          const matchingHistory = this.historyState().find((entry) => entry.bookId === book.id);
          return matchingHistory ? { ...book, progress: Math.round(matchingHistory.progress), lastWpm: matchingHistory.wpm } : book;
        }));
        this.historyState.update((history) => history.filter((entry) => books.some((book) => book.id === entry.bookId)));
      },
      error: () => this.booksState.set([])
    });
  }

  private loadReadingHistory(): void {
    this.http.get<ReadingHistoryResponse>(`${API_BASE_URL}/reading/history`).subscribe({
      next: (response) => {
        this.historyState.set(response.items);
        this.booksState.update((books) => books.map((book) => {
          const matchingHistory = response.items.find((entry) => entry.bookId === book.id);
          return matchingHistory ? { ...book, progress: Math.round(matchingHistory.progress), lastWpm: matchingHistory.wpm } : book;
        }));
      },
      error: () => this.historyState.set([])
    });
  }

  private loadTrainingOverview(): void {
    this.http.get<TrainingOverviewResponse>(`${API_BASE_URL}/training/overview`).subscribe({
      next: (overview) => this.applyTrainingOverview(overview),
      error: () => {
        this.trainingExercisesState.set([]);
        this.trainingSessionsState.set([]);
        this.weeklyTrainingProgressState.set(0);
      }
    });
  }

  private loadAchievements(): void {
    this.http.get<AchievementResponse>(`${API_BASE_URL}/achievements`).subscribe({
      next: (response) => this.achievementsState.set(response.items.map((item) => this.mapAchievement(item))),
      error: () => this.achievementsState.set([])
    });
  }

  private loadUsers(): void {
    this.http.get<UserSummary[]>(`${API_BASE_URL}/users`).subscribe({
      next: (users) => this.usersState.set(users),
      error: () => this.usersState.set([])
    });
  }

  private applyTrainingOverview(overview: TrainingOverviewResponse): void {
    this.trainingExercisesState.set(overview.exercises.map((item) => this.mapTrainingExercise(item)));
    this.trainingSessionsState.set(overview.recentSessions.map((item) => this.mapTrainingSession(item)));
    this.weeklyTrainingProgressState.set(overview.weeklyProgress);
  }

  private buildBookFormData(input: BookUpsertInput, file: File | null): FormData {
    const formData = new FormData();
    formData.append('title', input.title.trim());
    formData.append('author', input.author.trim());
    formData.append('description', input.description.trim());
    formData.append('fileType', input.fileType);
    formData.append('favorite', String(input.favorite));
    formData.append('archived', String(input.archived ?? false));
    formData.append('publicBook', String(input.publicBook));

    if (input.categoryId !== null) {
      formData.append('categoryId', String(input.categoryId));
    }

    if (input.content.trim()) {
      formData.append('contentText', input.content.trim());
    }

    if (file) {
      formData.append('file', file);
      formData.append('filePath', file.name);
    }

    return formData;
  }

  private mapBookResponse(response: BookApiResponse): BookSummary {
    const currentBook = this.getBook(response.id);
    const matchingHistory = this.historyState().find((entry) => entry.bookId === response.id);
    return {
      id: response.id,
      title: response.title,
      author: response.author ?? 'Autor nao informado',
      categoryId: response.categoryId,
      category: response.categoryName ?? 'Sem categoria',
      progress: matchingHistory ? Math.round(matchingHistory.progress) : (currentBook?.progress ?? 0),
      favorite: response.favorite,
      publicBook: response.publicBook,
      archived: response.archived,
      fileType: response.fileType,
      description: response.description ?? 'Sem descricao.',
      content: response.contentText ?? '',
      lastWpm: matchingHistory?.wpm ?? (currentBook?.lastWpm ?? 0),
      filePath: response.filePath
    };
  }

  private mapTrainingExercise(response: TrainingExerciseApiResponse): TrainingExercise {
    return {
      id: response.id,
      key: response.key,
      name: response.name,
      description: response.description,
      helper: response.helper,
      targetWpm: response.targetWpm,
      unlocked: response.unlocked,
      completedCount: response.completedCount,
      lastCompletedAt: response.lastCompletedAt
    };
  }

  private mapTrainingSession(response: TrainingSessionApiResponse): TrainingSessionSummary {
    return {
      id: response.id,
      exerciseKey: response.exerciseKey,
      exerciseName: response.exerciseName,
      targetWpm: response.targetWpm,
      achievedWpm: response.achievedWpm,
      durationSeconds: response.durationSeconds,
      completed: response.completed,
      createdAt: response.createdAt
    };
  }

  private mapAchievement(response: AchievementApiResponse): AchievementSummary {
    return {
      id: response.id,
      name: response.name,
      description: response.description,
      unlocked: response.unlocked,
      achievedAt: response.achievedAt
    };
  }

  private toBookUpsertInput(book: BookSummary, overrides: Partial<BookSummary> = {}): BookUpsertInput {
    const nextBook = { ...book, ...overrides };
    return {
      title: nextBook.title,
      author: nextBook.author,
      categoryId: nextBook.categoryId,
      fileType: nextBook.fileType,
      description: nextBook.description,
      content: nextBook.content,
      favorite: nextBook.favorite,
      publicBook: nextBook.publicBook,
      archived: nextBook.archived
    };
  }

  private formatRole(role: UserSummary['role']): string {
    if (role === 'ROLE_ADMIN') {
      return 'Administrador';
    }
    if (role === 'ROLE_EDITOR') {
      return 'Editor';
    }
    return 'Usuario';
  }

  private formatPlan(planType: UserSummary['planType']): string {
    if (planType === 'PREMIUM_YEARLY') {
      return 'Plano Premium Yearly';
    }
    if (planType === 'PREMIUM_MONTHLY') {
      return 'Plano Premium Monthly';
    }
    return 'Plano Free';
  }
}