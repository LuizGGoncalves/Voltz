import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ClienteService } from '../../core/services/cliente.service';
import { CadastroPendente } from '../../core/models/cliente.model';
import {
  StatusBadgeComponent, EmptyStateComponent, DocumentoPipe,
  SkeletonComponent, CalloutComponent
} from '../../shared';

@Component({
  selector: 'app-pendentes',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatTableModule, MatPaginatorModule, MatButtonToggleModule, MatIconModule, MatTooltipModule,
    StatusBadgeComponent, EmptyStateComponent, DocumentoPipe,
    SkeletonComponent, CalloutComponent
  ],
  template: `
    <app-callout icon="schedule" variant="brand">
      Cadastros que aguardam validação do ViaCEP. São processados automaticamente quando o serviço estiver disponível.
    </app-callout>

    <div class="filter-bar">
      <mat-button-toggle-group [(ngModel)]="filtroStatus" (change)="carregar()">
        <mat-button-toggle value="">Todos</mat-button-toggle>
        <mat-button-toggle value="PENDENTE">Pendente</mat-button-toggle>
        <mat-button-toggle value="PROCESSADO">Processado</mat-button-toggle>
        <mat-button-toggle value="REJEITADO">Rejeitado</mat-button-toggle>
        <mat-button-toggle value="FALHA">Falha</mat-button-toggle>
      </mat-button-toggle-group>
    </div>

    @if (loading()) {
      <div class="table-card">
        <div class="card-header"><h3>Fila de pendentes</h3></div>
        <app-skeleton [rowCount]="6" [columns]="[1, 2, 1, 1, 2, 1, 2]" />
      </div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (pendentes().length === 0) {
      <app-empty-state icon="check_circle" title="Nenhum cadastro pendente" subtitle="Todos os cadastros foram processados." />
    } @else {
      <div class="table-card">
        <div class="card-header">
          <h3>Fila de pendentes <span class="count-badge">{{ total() }}</span></h3>
        </div>
        <table mat-table [dataSource]="pendentes()" class="full-width">
          <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let p" class="mono">{{ p.id }}</td></ng-container>
          <ng-container matColumnDef="documento"><th mat-header-cell *matHeaderCellDef>Documento</th><td mat-cell *matCellDef="let p" class="mono">{{ p.documento | documento }}</td></ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let p"><app-status-badge [status]="p.status" /></td>
          </ng-container>
          <ng-container matColumnDef="motivo"><th mat-header-cell *matHeaderCellDef>Motivo</th><td mat-cell *matCellDef="let p">{{ p.motivo || '—' }}</td></ng-container>
          <ng-container matColumnDef="tentativas"><th mat-header-cell *matHeaderCellDef>Tentativas</th><td mat-cell *matCellDef="let p" class="mono">{{ p.tentativas }}</td></ng-container>
          <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Criado em</th><td mat-cell *matCellDef="let p" class="mono">{{ p.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
          <ng-container matColumnDef="cliente">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let p">
              @if (p.clienteId) {
                <a [routerLink]="['/clientes', p.clienteId]" class="link" matTooltip="Ver cliente criado">
                  <mat-icon class="link-icon">open_in_new</mat-icon> Cliente #{{ p.clienteId }}
                </a>
              } @else {
                <span class="text-faint">—</span>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="colunas"></tr>
          <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
        </table>
        <mat-paginator [length]="total()" [pageSize]="20" [pageIndex]="page" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
      </div>
    }
  `,
  styles: [`
    .filter-bar { margin-bottom: 20px; }
    .table-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      overflow: hidden;
    }
    .link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: var(--bolt-700);
      font-weight: 600;
      font-size: 13px;
      text-decoration: none;
      &:hover { text-decoration: underline; }
    }
    .link-icon { font-size: 16px; width: 16px; height: 16px; }
    .text-faint { color: var(--text-faint); }
  `]
})
export class PendentesComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  pendentes = signal<CadastroPendente[]>([]);
  total = signal(0);
  loading = signal(false);
  erro = signal<string | null>(null);
  page = 0;
  filtroStatus = '';
  colunas = ['id', 'documento', 'status', 'motivo', 'tentativas', 'createdAt', 'cliente'];

  constructor(private clienteService: ClienteService) {}
  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading.set(true);
    this.erro.set(null);
    this.clienteService.listarPendentes(this.page, 20, this.filtroStatus || undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => { this.pendentes.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
        error: () => { this.erro.set('Erro ao carregar pendentes.'); this.loading.set(false); }
      });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }
}
