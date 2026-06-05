import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../../core/services/cliente.service';
import { Cliente, UnidadeConsumidora } from '../../../core/models/cliente.model';
import { UcFormComponent } from '../uc-form/uc-form.component';
import { StatusBadgeComponent, EmptyStateComponent, DocumentoPipe } from '../../../shared';

@Component({
  selector: 'app-cliente-detalhe',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatSnackBarModule, MatDialogModule, MatProgressSpinnerModule,
    StatusBadgeComponent, EmptyStateComponent, DocumentoPipe
  ],
  template: `
    @if (loading()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (cliente()) {
      <div class="header">
        <h2>{{ cliente()!.nome }}</h2>
        <div class="actions">
          <a mat-stroked-button [routerLink]="['/clientes', cliente()!.id, 'editar']" aria-label="Editar cliente">
            <mat-icon>edit</mat-icon> Editar
          </a>
          <a mat-button routerLink="/clientes">Voltar</a>
        </div>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="info-grid">
            <div><strong>Documento:</strong> {{ cliente()!.documento | documento }} ({{ cliente()!.tipoDocumento }})</div>
            <div><strong>Status:</strong> <app-status-badge [status]="cliente()!.ativo ? 'ativo' : 'inativo'" [label]="cliente()!.ativo ? 'Ativo' : 'Inativo'" /></div>
            <div><strong>Endereço:</strong> {{ cliente()!.endereco.logradouro }}, {{ cliente()!.endereco.numero }} — {{ cliente()!.endereco.cidade }}/{{ cliente()!.endereco.uf }}</div>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="uc-header">
        <h3>Unidades Consumidoras</h3>
        <button mat-raised-button color="primary" (click)="abrirFormUC()" aria-label="Nova unidade consumidora">
          <mat-icon>add</mat-icon> Nova UC
        </button>
      </div>

      @if (ucs().length === 0) {
        <app-empty-state icon="business" message="Nenhuma unidade consumidora. Adicione a primeira!" />
      } @else {
        <table mat-table [dataSource]="ucs()" class="full-width">
          <ng-container matColumnDef="nome"><th mat-header-cell *matHeaderCellDef>Nome</th><td mat-cell *matCellDef="let uc">{{ uc.nome }}</td></ng-container>
          <ng-container matColumnDef="instalacao"><th mat-header-cell *matHeaderCellDef>Nº Instalação</th><td mat-cell *matCellDef="let uc">{{ uc.numeroInstalacao }}</td></ng-container>
          <ng-container matColumnDef="endereco"><th mat-header-cell *matHeaderCellDef>Endereço</th><td mat-cell *matCellDef="let uc">{{ uc.endereco.cidade }}/{{ uc.endereco.uf }}</td></ng-container>
          <ng-container matColumnDef="acoes">
            <th mat-header-cell *matHeaderCellDef>Ações</th>
            <td mat-cell *matCellDef="let uc">
              <button mat-icon-button (click)="abrirFormUC(uc)" aria-label="Editar UC"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button color="warn" (click)="removerUC(uc)" aria-label="Remover UC"><mat-icon>delete</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="['nome', 'instalacao', 'endereco', 'acoes']"></tr>
          <tr mat-row *matRowDef="let row; columns: ['nome', 'instalacao', 'endereco', 'acoes'];"></tr>
        </table>
      }
    }
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .actions { display: flex; gap: 8px; }
    .info-grid { display: flex; flex-direction: column; gap: 8px; }
    .uc-header { display: flex; justify-content: space-between; align-items: center; margin: 24px 0 16px; }
    .full-width { width: 100%; }
    .center { display: flex; justify-content: center; padding: 48px; }
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
    const ref = this.dialog.open(UcFormComponent, { width: '600px', data: { clienteId: this.clienteId, uc } });
    ref.afterClosed().subscribe(result => { if (result) this.carregar(); });
  }

  removerUC(uc: UnidadeConsumidora) {
    if (!confirm(`Remover "${uc.nome}"?`)) return;
    this.clienteService.removerUC(this.clienteId, uc.id).subscribe({
      next: () => { this.snackBar.open('UC removida', 'OK', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao remover', 'OK', { duration: 3000 })
    });
  }
}
