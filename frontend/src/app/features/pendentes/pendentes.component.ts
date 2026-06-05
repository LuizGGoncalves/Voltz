import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
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
    CommonModule, FormsModule,
    MatTableModule, MatPaginatorModule, MatButtonToggleModule,
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
        <app-skeleton [rowCount]="4" [columns]="[1, 2, 1, 2, 1, 2]" />
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
          <tr mat-header-row *matHeaderRowDef="colunas"></tr>
          <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
        </table>
        <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
      </div>
    }
  `,
  styles: [`
    .filter-bar {
      margin-bottom: 20px;
    }
    .table-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      overflow: hidden;
    }
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
  colunas = ['id', 'documento', 'status', 'motivo', 'tentativas', 'createdAt'];

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
