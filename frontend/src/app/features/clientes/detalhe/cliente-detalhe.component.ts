import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClienteService } from '../../../core/services/cliente.service';
import { Cliente, UnidadeConsumidora } from '../../../core/models/cliente.model';
import { UcFormComponent } from '../uc-form/uc-form.component';
import {
  StatusBadgeComponent, EmptyStateComponent, DocumentoPipe, AvatarComponent,
  ConfirmDialogComponent, ConfirmDialogData, SkeletonComponent
} from '../../../shared';

@Component({
  selector: 'app-cliente-detalhe',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatButtonModule, MatIconModule, MatSnackBarModule, MatDialogModule, MatTooltipModule,
    StatusBadgeComponent, EmptyStateComponent, DocumentoPipe, AvatarComponent, SkeletonComponent
  ],
  template: `
    @if (loading()) {
      <div class="center"><app-skeleton [rowCount]="3" [columns]="[3, 2, 1]" /></div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (cliente()) {
      <!-- Header com avatar -->
      <div class="detail-header">
        <div class="detail-header-left">
          <app-avatar [name]="cliente()!.nome" [size]="52" />
          <div class="detail-title">
            <h2>{{ cliente()!.nome }}</h2>
            <div class="detail-meta">
              <span class="mono">{{ cliente()!.documento | documento }}</span>
              <span class="doc-type">{{ cliente()!.tipoDocumento }}</span>
              <app-status-badge [status]="cliente()!.ativo ? 'ativo' : 'inativo'" />
            </div>
          </div>
        </div>
        <div class="detail-actions">
          <a mat-button [routerLink]="['/clientes', cliente()!.id, 'editar']" class="btn-soft">
            <mat-icon>edit</mat-icon> Editar
          </a>
          <a mat-button routerLink="/clientes" class="btn-ghost">
            <mat-icon>arrow_back</mat-icon> Voltar
          </a>
        </div>
      </div>

      <!-- Info cards grid -->
      <div class="info-grid">
        <div class="info-card">
          <div class="info-label">Endereço</div>
          <div class="info-value">
            {{ cliente()!.endereco.logradouro }}, {{ cliente()!.endereco.numero }}
            @if (cliente()!.endereco.complemento) {
              — {{ cliente()!.endereco.complemento }}
            }
          </div>
          <div class="info-sub">{{ cliente()!.endereco.bairro }} · {{ cliente()!.endereco.cidade }}/{{ cliente()!.endereco.uf }}</div>
          <div class="info-sub mono">CEP {{ cliente()!.endereco.cep }}</div>
        </div>
      </div>

      <!-- UCs section -->
      <div class="uc-section">
        <div class="uc-header">
          <h3>Unidades Consumidoras <span class="count-badge">{{ ucs().length }}</span></h3>
          <button mat-raised-button color="primary" (click)="abrirFormUC()">
            <mat-icon>add</mat-icon> Nova UC
          </button>
        </div>

        @if (ucs().length === 0) {
          <app-empty-state icon="business" title="Nenhuma unidade consumidora" subtitle="Adicione a primeira unidade consumidora deste cliente." />
        } @else {
          <div class="uc-cards">
            @for (uc of ucs(); track uc.id) {
              <div class="uc-card">
                <div class="uc-card-header">
                  <div class="uc-card-title">
                    <mat-icon class="uc-icon">business</mat-icon>
                    <div>
                      <div class="uc-name">{{ uc.nome }}</div>
                      <div class="uc-install mono">Nº {{ uc.numeroInstalacao }}</div>
                    </div>
                  </div>
                  <div class="uc-card-actions">
                    <button mat-icon-button (click)="abrirFormUC(uc)" matTooltip="Editar">
                      <mat-icon>edit</mat-icon>
                    </button>
                    <button mat-icon-button (click)="removerUC(uc)" matTooltip="Remover" class="btn-danger">
                      <mat-icon>delete</mat-icon>
                    </button>
                  </div>
                </div>
                <div class="uc-card-body">
                  <div class="uc-addr">
                    {{ uc.endereco.logradouro }}, {{ uc.endereco.numero }} — {{ uc.endereco.cidade }}/{{ uc.endereco.uf }}
                  </div>
                </div>
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: [`
    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      gap: 16px;
      flex-wrap: wrap;
    }
    .detail-header-left {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .detail-title h2 {
      margin: 0 0 4px;
      font-size: 22px;
    }
    .detail-meta {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: var(--text-soft);
    }
    .doc-type {
      padding: 2px 8px;
      border-radius: var(--r-pill);
      background: var(--surface-2);
      font-size: 11px;
      font-weight: 700;
      color: var(--text-faint);
    }
    .detail-actions {
      display: flex;
      gap: 8px;
    }
    .btn-soft {
      background: var(--bolt-tint) !important;
      color: var(--bolt-700) !important;
      border-radius: var(--r-pill) !important;
    }
    .btn-ghost {
      border: 1px solid var(--border-strong) !important;
      border-radius: var(--r-pill) !important;
      color: var(--text) !important;
    }

    .info-grid {
      display: grid;
      grid-template-columns: 1fr;
      gap: 14px;
      margin-bottom: 28px;
    }
    .info-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      padding: 18px 22px;
      box-shadow: var(--sh-1);
    }
    .info-label {
      font-size: 12px;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
      color: var(--text-faint);
      margin-bottom: 6px;
    }
    .info-value {
      font-size: 15px;
      font-weight: 600;
      color: var(--text);
    }
    .info-sub {
      font-size: 13px;
      color: var(--text-soft);
      margin-top: 2px;
    }

    .uc-section { margin-top: 4px; }
    .uc-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
    }
    .uc-header h3 {
      display: flex;
      align-items: center;
    }

    .uc-cards {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
      gap: 14px;
    }
    .uc-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      overflow: hidden;
      transition: box-shadow var(--dur) var(--ease);
      &:hover { box-shadow: var(--sh-2); }
    }
    .uc-card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 16px 18px 0;
    }
    .uc-card-title {
      display: flex;
      align-items: flex-start;
      gap: 10px;
    }
    .uc-icon {
      color: var(--bolt-400);
      font-size: 22px;
      width: 22px;
      height: 22px;
      margin-top: 2px;
    }
    .uc-name {
      font-weight: 600;
      font-size: 15px;
      color: var(--text);
    }
    .uc-install {
      font-size: 12.5px;
      color: var(--text-faint);
    }
    .uc-card-actions {
      display: flex;
      gap: 2px;
    }
    .uc-card-body {
      padding: 10px 18px 16px;
    }
    .uc-addr {
      font-size: 13px;
      color: var(--text-soft);
    }

    .center { padding: 48px; }
  `]
})
export class ClienteDetalheComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  cliente = signal<Cliente | null>(null);
  ucs = signal<UnidadeConsumidora[]>([]);
  loading = signal(true);
  erro = signal<string | null>(null);
  clienteId!: number;

  constructor(
    private route: ActivatedRoute,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.clienteId = +this.route.snapshot.paramMap.get('id')!;
    this.carregar();
  }

  carregar() {
    this.loading.set(true);
    this.erro.set(null);
    this.clienteService.buscarPorId(this.clienteId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: c => { this.cliente.set(c); this.loading.set(false); },
        error: () => { this.erro.set('Erro ao carregar cliente.'); this.loading.set(false); }
      });
    this.clienteService.listarUCs(this.clienteId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: ucs => this.ucs.set(ucs), error: () => {} });
  }

  abrirFormUC(uc?: UnidadeConsumidora) {
    const ref = this.dialog.open(UcFormComponent, { width: '640px', data: { clienteId: this.clienteId, uc } });
    ref.afterClosed().subscribe(result => { if (result) this.carregar(); });
  }

  removerUC(uc: UnidadeConsumidora) {
    const data: ConfirmDialogData = {
      title: 'Remover unidade consumidora',
      message: `Deseja remover "${uc.nome}" (Nº ${uc.numeroInstalacao})?`,
      icon: 'delete',
      confirmLabel: 'Remover',
      variant: 'danger'
    };
    this.dialog.open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed().subscribe(confirmed => {
        if (!confirmed) return;
        this.clienteService.removerUC(this.clienteId, uc.id).subscribe({
          next: () => { this.snackBar.open('UC removida com sucesso', 'OK', { duration: 4000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao remover UC', 'OK', { duration: 4000 })
        });
      });
  }
}
