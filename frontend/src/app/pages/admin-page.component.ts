import { Component, inject } from '@angular/core';

import { PlatformService } from '../data/platform.service';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  template: `
    <section class="admin-grid">
      @for (metric of metrics(); track metric.label) {
        <section class="card-surface metric-card">
          <strong>{{ metric.value }}</strong>
          <span>{{ metric.label }}</span>
          <small>{{ metric.helper }}</small>
        </section>
      }

      <section class="card-surface full-width panel-card">
        <header class="card-head">
          <h2 class="section-title">Gestao de usuarios e catalogo publico</h2>
        </header>

        @if (canManageUsers()) {
          <div class="table-like">
            <div class="table-head">
              <span>Usuario</span>
              <span>Status</span>
              <span>Plano</span>
              <span>Perfil</span>
            </div>

            @for (user of users(); track user.id) {
              <div class="table-row">
                <strong>{{ user.name }}</strong>
                <span class="status-pill" [class.success]="user.active" [class.warning]="!user.active">{{ user.active ? 'Ativo' : 'Inativo' }}</span>
                <span>{{ user.planType }}</span>
                <span>{{ user.role }}</span>
              </div>
            }
          </div>
        } @else {
          <div class="table-like">
            <div class="table-head">
              <span>Escopo</span>
              <span>Acesso</span>
              <span>Fonte</span>
              <span>Observacao</span>
            </div>

            <div class="table-row">
              <strong>Catalogo publico</strong>
              <span class="status-pill success">Permitido</span>
              <span>Livros persistidos</span>
              <span>Editor acompanha o acervo publico e a distribuicao por categorias.</span>
            </div>

            <div class="table-row">
              <strong>Gestao de usuarios</strong>
              <span class="status-pill muted">Somente admin</span>
              <span>API /users</span>
              <span>Administradores veem usuarios, planos e perfis completos.</span>
            </div>
          </div>
        }
      </section>
    </section>
  `,
  styles: [`
    .admin-grid {
      display: grid;
      grid-template-columns: repeat(12, minmax(0, 1fr));
      gap: 1.5rem;
    }

    .metric-card {
      grid-column: span 3;
      padding: 1.2rem;
      border-radius: 24px;
      display: grid;
      gap: 0.4rem;
    }

    .metric-card strong {
      font-size: 2rem;
    }

    .metric-card span,
    .metric-card small {
      color: var(--ld-muted);
    }

    .panel-card {
      grid-column: span 12;
      padding: 1rem;
      border-radius: 24px;
    }

    .card-head {
      margin-bottom: 1rem;
    }

    .table-like {
      display: grid;
      gap: 0.8rem;
    }

    .table-head,
    .table-row {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr 2fr;
      gap: 1rem;
      align-items: center;
      padding: 1rem;
      border-radius: 18px;
      background: rgba(16, 42, 67, 0.05);
    }

    .table-head {
      font-weight: 700;
    }

    @media (max-width: 992px) {
      .metric-card,
      .panel-card {
        grid-column: span 12;
      }

      .table-head,
      .table-row {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AdminPageComponent {
  private readonly platformService = inject(PlatformService);

  protected readonly metrics = this.platformService.adminMetrics;
  protected readonly users = this.platformService.users;
  protected readonly canManageUsers = this.platformService.canManageUsers;
}