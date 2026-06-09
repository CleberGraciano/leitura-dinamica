import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';

import { AuthSessionService } from '../core/auth-session.service';

@Component({
  selector: 'app-auth-page',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="auth-page container py-5">
      <div class="row align-items-center g-4">
        <div class="col-lg-6">
          <div class="hero-copy">
            <span class="eyebrow">Leitura Dinamica</span>
            <h1>Plataforma multiplataforma para leitura, estudo e evolucao de velocidade.</h1>
            <p>
              Importe livros, acompanhe estatisticas, marque favoritos e treine ORP com uma unica base Angular.
            </p>
            <div class="pill-row">
              <span>Angular</span>
              <span>Spring Boot</span>
              <span>MySQL</span>
              <span>Docker</span>
            </div>
          </div>
        </div>

        <div class="col-lg-5 offset-lg-1">
          <div class="card-surface auth-card">
            <div class="auth-header">
              <span class="eyebrow small-eye">Acesso seguro com JWT</span>
              <h2>{{ mode() === 'login' ? 'Entrar' : 'Criar conta' }}</h2>
            </div>

            <div class="tab-row">
              <button type="button" class="tab-btn" [class.active]="mode() === 'login'" (click)="changeMode('login')">Login</button>
              <button type="button" class="tab-btn" [class.active]="mode() === 'register'" (click)="changeMode('register')">Cadastro</button>
            </div>

            <form class="form-stack" [formGroup]="form" (ngSubmit)="submit()">
              @if (mode() === 'register') {
                <label class="field-group">
                  <span>Nome</span>
                  <input class="field-ui" formControlName="name" placeholder="Seu nome" />
                </label>
              }

              <label class="field-group">
                <span>Email</span>
                <input class="field-ui" formControlName="email" placeholder="voce@email.com" />
              </label>

              <label class="field-group">
                <span>Senha</span>
                <input class="field-ui" type="password" formControlName="password" placeholder="********" />
              </label>

              @if (errorMessage()) {
                <p class="error-box">{{ errorMessage() }}</p>
              }

              <button class="btn-ui btn-primary w-100" type="submit" [disabled]="form.invalid || submitting()">
                {{ submitting() ? 'Processando...' : (mode() === 'login' ? 'Acessar dashboard' : 'Criar conta') }}
              </button>
            </form>

            <p class="helper-copy">A sessao fica persistida no navegador e libera o CRUD real da biblioteca por usuario.</p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .auth-shell {
      min-height: 100vh;
      display: flex;
      align-items: center;
    }

    .auth-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
    }

    .hero-copy h1 {
      font-size: clamp(2.8rem, 6vw, 4.75rem);
      line-height: 0.95;
      margin-bottom: 1rem;
      max-width: 12ch;
    }

    .hero-copy p {
      color: var(--ld-muted);
      font-size: 1.05rem;
      max-width: 54ch;
    }

    .eyebrow {
      display: inline-block;
      margin-bottom: 1rem;
      padding: 0.35rem 0.8rem;
      border-radius: 999px;
      background: rgba(255, 122, 24, 0.14);
      color: var(--ld-accent);
      font-weight: 700;
      letter-spacing: 0.08em;
      text-transform: uppercase;
      font-size: 0.78rem;
    }

    .small-eye {
      margin-bottom: 0.4rem;
      font-size: 0.72rem;
    }

    .pill-row {
      display: flex;
      flex-wrap: wrap;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }

    .pill-row span {
      padding: 0.55rem 0.95rem;
      border-radius: 999px;
      background: rgba(16, 42, 67, 0.08);
      color: var(--ld-ink);
      font-weight: 600;
    }

    .auth-card {
      padding: 1.75rem;
      display: grid;
      gap: 1.25rem;
    }

    .auth-header h2 {
      margin: 0;
      font-size: 2rem;
    }

    .tab-row {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 0.65rem;
    }

    .tab-btn {
      padding: 0.85rem 1rem;
      border-radius: 14px;
      border: 1px solid rgba(16, 42, 67, 0.1);
      background: rgba(16, 42, 67, 0.04);
      font-weight: 700;
      cursor: pointer;
    }

    .tab-btn.active {
      background: rgba(255, 122, 24, 0.12);
      color: var(--ld-accent);
      border-color: rgba(255, 122, 24, 0.22);
    }

    .form-stack {
      display: grid;
      gap: 1rem;
    }

    .field-group {
      display: grid;
      gap: 0.45rem;
      font-weight: 600;
    }

    .error-box {
      margin: 0;
      padding: 0.85rem 1rem;
      border-radius: 14px;
      background: rgba(180, 35, 24, 0.1);
      color: #8f1f14;
      font-weight: 600;
    }

    .helper-copy {
      margin: 0;
      color: var(--ld-muted);
    }
  `]
})
export class AuthPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly authSession = inject(AuthSessionService);

  readonly mode = signal<'login' | 'register'>('login');
  readonly submitting = signal(false);
  readonly errorMessage = signal('');

  readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.minLength(3)]],
    email: ['demo@leituradinamica.dev', [Validators.required, Validators.email]],
    password: ['12345678', [Validators.required, Validators.minLength(8)]]
  });

  changeMode(value: 'login' | 'register'): void {
    this.mode.set(value ?? 'login');
    this.errorMessage.set('');
    if (this.mode() === 'login') {
      this.form.controls.name.setValue('');
    }
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();
    const request$ = this.mode() === 'login'
      ? this.authSession.login({ email: payload.email, password: payload.password })
      : this.authSession.register({ name: payload.name, email: payload.email, password: payload.password });

    this.submitting.set(true);
    this.errorMessage.set('');

    request$.pipe(finalize(() => this.submitting.set(false))).subscribe({
      next: () => this.router.navigateByUrl('/app/dashboard'),
      error: (error: HttpErrorResponse) => {
        this.errorMessage.set(error.error?.message ?? 'Nao foi possivel autenticar no momento.');
      }
    });
  }
}