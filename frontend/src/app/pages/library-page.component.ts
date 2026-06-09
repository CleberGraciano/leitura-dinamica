import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';

import { BookSummary, BookUpsertInput, PlatformService } from '../data/platform.service';

@Component({
  selector: 'app-library-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="page-grid">
      <section class="card-surface section-card full-width">
        <div class="library-shell">
          <div class="library-head">
            <div>
              <h2 class="section-title">Biblioteca de leituras</h2>
              <p class="section-copy">Cadastre livros com upload real de PDF, EPUB ou TXT e persista tudo no backend por usuario autenticado.</p>
            </div>

            <button class="btn-ld-secondary" type="button" (click)="resetForm()">
              {{ editingBookId() === null ? 'Limpar formulario' : 'Cancelar edicao' }}
            </button>
          </div>

          <div class="library-layout">
            <form class="editor-card" [formGroup]="bookForm" (ngSubmit)="submitBook()">
              <div class="field-grid">
                <label class="field">
                  <span>Titulo</span>
                  <input formControlName="title" placeholder="Ex.: Clean Architecture" />
                </label>

                <label class="field">
                  <span>Autor</span>
                  <input formControlName="author" placeholder="Nome do autor" />
                </label>

                <label class="field">
                  <span>Categoria</span>
                  <select formControlName="categoryId">
                    <option value="">Sem categoria</option>
                    @for (category of categories(); track category.id) {
                      <option [value]="category.id">{{ category.name }}</option>
                    }
                  </select>
                </label>

                <label class="field">
                  <span>Tipo</span>
                  <select formControlName="fileType">
                    @for (option of fileTypeOptions; track option) {
                      <option [value]="option">{{ option }}</option>
                    }
                  </select>
                </label>

                <label class="field field-wide">
                  <span>Arquivo</span>
                  <input type="file" accept=".pdf,.epub,.txt,.text" (change)="handleFileSelection($event)" />
                  <small class="helper-text">{{ selectedFileName() || 'Nenhum arquivo selecionado. Voce tambem pode colar o conteudo manualmente abaixo.' }}</small>
                </label>

                <label class="field field-wide">
                  <span>Descricao</span>
                  <textarea rows="3" formControlName="description" placeholder="Resumo curto do livro"></textarea>
                </label>

                <label class="field field-wide">
                  <span>Conteudo</span>
                  <textarea rows="8" formControlName="content" placeholder="Opcional quando voce envia um arquivo. Obrigatorio se nao houver upload."></textarea>
                </label>
              </div>

              <div class="toggle-list">
                <label class="check-row">
                  <input type="checkbox" formControlName="favorite" />
                  <span>Marcar como favorito</span>
                </label>
                <label class="check-row">
                  <input type="checkbox" formControlName="publicBook" />
                  <span>Disponibilizar como livro publico</span>
                </label>
              </div>

              @if (feedbackMessage()) {
                <p class="feedback-box" [class.error]="feedbackKind() === 'error'">{{ feedbackMessage() }}</p>
              }

              <div class="form-actions">
                <button class="btn-ld" type="submit" [disabled]="!canSubmit()">
                  {{ submitting() ? 'Salvando...' : (editingBookId() === null ? 'Inserir livro' : 'Salvar alteracoes') }}
                </button>
                <p class="helper-text">{{ editingBookId() === null ? 'O upload do arquivo sera processado no backend e convertido em texto para o leitor.' : 'Edicao persistida no backend para o usuario autenticado.' }}</p>
              </div>
            </form>

            <div class="catalog-card">
              <div class="d-flex flex-wrap gap-2 mb-4">
                @for (category of categories(); track category.id) {
                  <button class="tag category-tag" type="button" (click)="toggleCategoryFavorite(category.id)">
                    {{ category.name }}
                  </button>
                }
              </div>

              <div class="book-grid">
                @for (book of books(); track book.id) {
                  <article class="book-card" [class.selected]="editingBookId() === book.id">
                    <div>
                      <div class="title-row">
                        <span class="type-tag">{{ book.fileType }}</span>
                        @if (book.publicBook) {
                          <span class="status-pill muted">Publico</span>
                        }
                      </div>
                      <h3>{{ book.title }}</h3>
                      <p>{{ book.author }} • {{ book.category }}</p>
                      <small>{{ book.description }}</small>
                    </div>

                    <div class="book-meta">
                      <span class="status-pill success">{{ book.progress }}% concluido</span>
                      <span class="status-pill muted">{{ book.lastWpm }} WPM</span>
                    </div>

                    <div class="book-actions">
                      <button class="btn-ld-ghost" type="button" (click)="editBook(book)">Editar</button>
                      <button class="btn-ld-ghost" type="button" (click)="toggleFavorite(book.id)">{{ book.favorite ? '♥ Favorito' : '♡ Favoritar' }}</button>
                      <button class="btn-ld-ghost" type="button" (click)="toggleArchive(book.id)">{{ book.archived ? 'Desarquivar' : 'Arquivar' }}</button>
                      <button class="btn-ld-ghost danger" type="button" (click)="deleteBook(book.id)">Excluir</button>
                    </div>
                  </article>
                }
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="card-surface section-card">
        <header class="card-head">
          <h2 class="section-title">Favoritos</h2>
        </header>
        @for (book of favoriteBooks(); track book.id) {
          <div class="compact-row">
            <strong>{{ book.title }}</strong>
            <span>{{ book.category }}</span>
          </div>
        }
      </section>

      <section class="card-surface section-card">
        <header class="card-head">
          <h2 class="section-title">Historico recente</h2>
        </header>
        @for (entry of history(); track entry.id) {
          <div class="compact-row">
            <strong>{{ entry.bookTitle }}</strong>
            <span>{{ entry.progress }}% • {{ entry.wpm }} WPM</span>
          </div>
        }
      </section>
    </section>
  `,
  styles: [`
    .page-grid {
      display: grid;
      grid-template-columns: repeat(12, minmax(0, 1fr));
      gap: 1.5rem;
    }

    .section-card {
      grid-column: span 6;
      padding: 1.2rem;
      border-radius: 24px;
    }

    .full-width {
      grid-column: span 12;
    }

    .card-head {
      margin-bottom: 1rem;
    }

    .library-shell {
      display: grid;
      gap: 1.5rem;
    }

    .library-head {
      display: flex;
      align-items: start;
      justify-content: space-between;
      gap: 1rem;
    }

    .section-copy,
    .helper-text {
      margin: 0.35rem 0 0;
      color: var(--ld-muted);
    }

    .library-layout {
      display: grid;
      grid-template-columns: minmax(320px, 380px) minmax(0, 1fr);
      gap: 1.25rem;
      align-items: start;
    }

    .editor-card,
    .catalog-card {
      padding: 1rem;
      border-radius: 22px;
      background: rgba(16, 42, 67, 0.04);
    }

    .field-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 0.85rem;
    }

    .field-wide {
      grid-column: 1 / -1;
    }

    .toggle-list {
      display: grid;
      gap: 0.65rem;
      margin: 1rem 0;
    }

    .check-row {
      display: flex;
      align-items: center;
      gap: 0.6rem;
      font-weight: 600;
    }

    .form-actions {
      display: grid;
      gap: 0.75rem;
      margin-top: 1rem;
    }

    .feedback-box {
      margin: 0;
      padding: 0.85rem 1rem;
      border-radius: 14px;
      background: rgba(0, 168, 150, 0.12);
      color: #0f6f66;
      font-weight: 600;
    }

    .feedback-box.error {
      background: rgba(180, 35, 24, 0.1);
      color: #8f1f14;
    }

    .category-tag {
      cursor: pointer;
      border: 0;
    }

    .book-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
      gap: 1rem;
    }

    .book-card {
      padding: 1rem;
      border-radius: 22px;
      background: rgba(16, 42, 67, 0.05);
      display: grid;
      gap: 1rem;
      transition: border-color 0.2s ease, transform 0.2s ease;
      border: 1px solid transparent;
    }

    .book-card.selected {
      border-color: rgba(255, 122, 24, 0.3);
      transform: translateY(-2px);
    }

    .book-card h3,
    .compact-row strong {
      margin: 0;
    }

    .book-card p,
    .book-card small,
    .compact-row span {
      color: var(--ld-muted);
    }

    .type-tag {
      display: inline-block;
      margin-bottom: 0.65rem;
      padding: 0.25rem 0.65rem;
      border-radius: 999px;
      background: rgba(255, 122, 24, 0.14);
      color: var(--ld-accent);
      font-weight: 700;
      font-size: 0.75rem;
    }

    .title-row {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
      align-items: center;
      margin-bottom: 0.65rem;
    }

    .book-meta,
    .book-actions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .danger {
      color: #a62c2c;
    }

    .compact-row {
      display: flex;
      justify-content: space-between;
      gap: 1rem;
      padding: 0.75rem 0;
      border-bottom: 1px solid rgba(16, 42, 67, 0.08);
    }

    .compact-row:last-child {
      border-bottom: 0;
    }

    @media (max-width: 992px) {
      .library-layout,
      .field-grid {
        grid-template-columns: 1fr;
      }

      .library-head {
        align-items: stretch;
      }

      .section-card,
      .full-width {
        grid-column: span 12;
      }
    }
  `]
})
export class LibraryPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly platformService = inject(PlatformService);
  private selectedFile: File | null = null;

  protected readonly editingBookId = signal<number | null>(null);
  protected readonly feedbackMessage = signal('');
  protected readonly feedbackKind = signal<'success' | 'error'>('success');
  protected readonly submitting = signal(false);
  protected readonly selectedFileName = signal('');
  protected readonly books = this.platformService.activeBooks;
  protected readonly categories = this.platformService.categories;
  protected readonly favoriteBooks = this.platformService.favoriteBooks;
  protected readonly history = this.platformService.history;
  protected readonly fileTypeOptions: BookSummary['fileType'][] = ['PDF', 'EPUB', 'TXT', 'TEXT'];
  protected readonly bookForm = this.formBuilder.nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    author: ['', [Validators.minLength(3)]],
    categoryId: [''],
    fileType: ['PDF' as BookSummary['fileType'], [Validators.required]],
    description: [''],
    content: ['', [Validators.minLength(30)]],
    favorite: [false],
    publicBook: [false]
  });
  protected readonly canSubmit = computed(() => {
    if (this.submitting()) {
      return false;
    }

    const { title, fileType, content } = this.bookForm.getRawValue();
    const hasSource = !!this.selectedFile || !!content.trim();
    return this.bookForm.controls.title.valid && !!fileType && hasSource && title.trim().length >= 3;
  });

  toggleFavorite(bookId: number): void {
    this.platformService.toggleFavorite(bookId);
  }

  toggleArchive(bookId: number): void {
    this.platformService.toggleArchive(bookId);
  }

  toggleCategoryFavorite(categoryId: number): void {
    this.platformService.toggleCategoryFavorite(categoryId);
  }

  handleFileSelection(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile = file;
    this.selectedFileName.set(file?.name ?? '');
    if (file) {
      this.bookForm.controls.fileType.setValue(this.inferFileType(file.name));
    }
  }

  editBook(book: BookSummary): void {
    this.editingBookId.set(book.id);
    this.selectedFile = null;
    this.selectedFileName.set(book.filePath ?? '');
    this.bookForm.setValue({
      title: book.title,
      author: book.author,
      categoryId: book.categoryId === null ? '' : String(book.categoryId),
      fileType: book.fileType,
      description: book.description,
      content: book.content,
      favorite: book.favorite,
      publicBook: book.publicBook
    });
    this.feedbackMessage.set('');
  }

  resetForm(): void {
    this.editingBookId.set(null);
    this.selectedFile = null;
    this.selectedFileName.set('');
    this.bookForm.reset({
      title: '',
      author: '',
      categoryId: '',
      fileType: 'PDF',
      description: '',
      content: '',
      favorite: false,
      publicBook: false
    });
  }

  submitBook(): void {
    if (this.bookForm.invalid || (!this.selectedFile && !this.bookForm.controls.content.value.trim())) {
      this.bookForm.markAllAsTouched();
      this.feedbackKind.set('error');
      this.feedbackMessage.set('Envie um arquivo ou informe um conteudo manual para o livro.');
      return;
    }

    const payload = this.toPayload();
    const request$ = this.editingBookId() === null
      ? this.platformService.createBook(payload, this.selectedFile)
      : this.platformService.updateBook(this.editingBookId()!, payload, this.selectedFile);

    this.submitting.set(true);
    this.feedbackMessage.set('');

    request$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: (book) => {
        this.feedbackKind.set('success');
        this.feedbackMessage.set(`Livro "${book.title}" salvo com sucesso.`);
        this.resetForm();
      },
      error: (error: HttpErrorResponse) => {
        this.feedbackKind.set('error');
        this.feedbackMessage.set(error.error?.message ?? 'Nao foi possivel salvar o livro.');
      }
    });
  }

  deleteBook(bookId: number): void {
    const book = this.platformService.getBook(bookId);
    if (!book) {
      return;
    }

    if (!confirm(`Excluir o livro "${book.title}"?`)) {
      return;
    }

    this.platformService.deleteBook(bookId).subscribe({
      next: () => {
        if (this.editingBookId() === bookId) {
          this.resetForm();
        }
        this.feedbackKind.set('success');
        this.feedbackMessage.set(`Livro "${book.title}" excluido com sucesso.`);
      },
      error: (error: HttpErrorResponse) => {
        this.feedbackKind.set('error');
        this.feedbackMessage.set(error.error?.message ?? 'Nao foi possivel excluir o livro.');
      }
    });
  }

  private toPayload(): BookUpsertInput {
    const rawValue = this.bookForm.getRawValue();
    return {
      title: rawValue.title,
      author: rawValue.author,
      categoryId: rawValue.categoryId ? Number(rawValue.categoryId) : null,
      fileType: rawValue.fileType,
      description: rawValue.description,
      content: rawValue.content,
      favorite: rawValue.favorite,
      publicBook: rawValue.publicBook,
      archived: this.editingBookId() === null ? false : (this.platformService.getBook(this.editingBookId()!)?.archived ?? false)
    };
  }

  private inferFileType(fileName: string): BookSummary['fileType'] {
    const extension = fileName.split('.').pop()?.toLowerCase();
    if (extension === 'pdf') {
      return 'PDF';
    }
    if (extension === 'epub') {
      return 'EPUB';
    }
    if (extension === 'txt') {
      return 'TXT';
    }
    return 'TEXT';
  }
}