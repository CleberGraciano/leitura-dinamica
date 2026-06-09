import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthSessionService } from '../core/auth-session.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  template: `
    <div class="shell-layout">
      <aside class="shell-sidebar card-surface">
        <div class="profile-box">
          <img alt="Usuario" [src]="avatarUrl()" />
          <strong>{{ userName() }}</strong>
          <span>{{ userPlan() }}</span>
          <span class="chip-ui chip-accent">{{ userRoleLabel() }}</span>
        </div>

        <nav class="nav-list">
          @for (item of items(); track item.path) {
            <a class="nav-item" [routerLink]="item.path" routerLinkActive="active-link">
              <span class="nav-mark">{{ item.short }}</span>
              <span>{{ item.label }}</span>
            </a>
          }
        </nav>
      </aside>

      <div class="shell-main">
        <header class="shell-topbar card-surface">
          <div>
            <strong>Plataforma de Speed Reading</strong>
            <p>CRUD de livros persistido por usuario autenticado via Spring Boot.</p>
          </div>
          <button class="btn-ui btn-outline" type="button" (click)="logout()">Sair</button>
        </header>

        <main class="content-wrap">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `,
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
    }

    .shell-layout {
      min-height: 100vh;
      display: grid;
      grid-template-columns: 310px minmax(0, 1fr);
      gap: 1.25rem;
      padding: 1.25rem;
    }

    .profile-box {
      display: grid;
      gap: 0.75rem;
      padding: 1.5rem;
      text-align: center;
    }

    .profile-box img {
      width: 72px;
      height: 72px;
      border-radius: 50%;
      margin: 0 auto;
    }

    .shell-sidebar {
      display: grid;
      gap: 1rem;
      align-content: start;
      padding: 0.75rem;
    }

    .nav-list {
      display: grid;
      gap: 0.45rem;
      padding: 0 0.5rem 0.75rem;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 0.8rem;
      padding: 0.9rem 1rem;
      border-radius: 16px;
      color: var(--ld-ink);
      border: 1px solid transparent;
    }

    .nav-mark {
      min-width: 2rem;
      height: 2rem;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      border-radius: 999px;
      background: rgba(16, 42, 67, 0.08);
      font-size: 0.72rem;
      font-weight: 700;
    }

    .active-link {
      background: rgba(255, 122, 24, 0.12);
      color: var(--ld-accent);
      border-color: rgba(255, 122, 24, 0.18);
      font-weight: 700;
    }

    .shell-main {
      display: grid;
      grid-template-rows: auto 1fr;
      gap: 1rem;
    }

    .shell-topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      padding: 1.25rem 1.5rem;
    }

    .shell-topbar p {
      margin: 0.25rem 0 0;
      color: var(--ld-muted);
    }

    .content-wrap {
      padding: 0.2rem;
    }

    @media (max-width: 992px) {
      .shell-layout {
        grid-template-columns: 1fr;
      }

      .shell-topbar {
        flex-direction: column;
        align-items: flex-start;
      }
    }
  `]
})
export class AppShellComponent {
  private readonly authSession = inject(AuthSessionService);
  private readonly router = inject(Router);

  protected readonly userName = computed(() => this.authSession.currentUser()?.name ?? 'Usuario');
  protected readonly userPlan = computed(() => {
    const planType = this.authSession.currentUser()?.planType ?? 'FREE';
    return planType === 'PREMIUM_YEARLY' ? 'Plano Premium Yearly' : planType === 'PREMIUM_MONTHLY' ? 'Plano Premium Monthly' : 'Plano Free';
  });
  protected readonly userRoleLabel = computed(() => {
    const role = this.authSession.currentUser()?.role ?? 'ROLE_USER';
    return role === 'ROLE_ADMIN' ? 'Administrador' : role === 'ROLE_EDITOR' ? 'Editor' : 'Usuario';
  });
  protected readonly canAccessAdmin = computed(() => {
    const role = this.authSession.currentUser()?.role;
    return role === 'ROLE_ADMIN' || role === 'ROLE_EDITOR';
  });
  protected readonly avatarUrl = computed(() => this.authSession.currentUser()?.photo || 'https://i.pravatar.cc/120?img=32');

  protected readonly items = computed(() => {
    const baseItems = [
      { label: 'Dashboard', path: '/app/dashboard', short: 'DB' },
      { label: 'Biblioteca', path: '/app/library', short: 'LB' },
      { label: 'Leitor', path: '/app/reader', short: 'RD' },
      { label: 'Treinamento', path: '/app/training', short: 'TR' }
    ];

    return this.canAccessAdmin() ? [...baseItems, { label: 'Admin', path: '/app/admin', short: 'AD' }] : baseItems;
  });

  logout(): void {
    this.authSession.logout();
    this.router.navigateByUrl('/auth');
  }
}