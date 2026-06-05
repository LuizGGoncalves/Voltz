import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ClienteService } from '../../core/services/cliente.service';
import { AnaliseMg } from '../../core/models/cliente.model';
import {
  StatusBadgeComponent, EmptyStateComponent,
  SkeletonComponent, CalloutComponent
} from '../../shared';

@Component({
  selector: 'app-analise-mg',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatTableModule, MatPaginatorModule,
    StatusBadgeComponent, EmptyStateComponent,
    SkeletonComponent, CalloutComponent
  ],
  template: `
    <app-callout icon="assessment" variant="accent">
      Clientes com unidades consumidoras em Minas Gerais registrados para análise futura.
      <span class="mg-tag" slot="right">MG</span>
    </app-callout>

    @if (loading()) {
      <div class="table-card">
        <div class="card-header"><h3>Análises MG</h3></div>
        <app-skeleton [rowCount]="3" [columns]="[1, 2, 2, 1, 2]" />
      </div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (analises().length === 0) {
      <app-empty-state icon="assessment" title="Nenhuma análise MG" subtitle="Não há clientes com UCs em Minas Gerais registrados para análise." />
    } @else {
      <div class="table-card">
        <div class="card-header">
          <h3>Análises MG <span class="count-badge">{{ total() }}</span></h3>
        </div>
        <table mat-table [dataSource]="analises()" class="full-width">
          <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let a" class="mono">{{ a.id }}</td></ng-container>
          <ng-container matColumnDef="clienteId">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let a">
              <a [routerLink]="['/clientes', a.clienteId]" class="link">Cliente #{{ a.clienteId }}</a>
            </td>
          </ng-container>
          <ng-container matColumnDef="ucId"><th mat-header-cell *matHeaderCellDef>UC</th><td mat-cell *matCellDef="let a" class="mono">UC #{{ a.unidadeConsumidoraId }}</td></ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let a"><app-status-badge [status]="a.status" /></td>
          </ng-container>
          <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Registrado em</th><td mat-cell *matCellDef="let a" class="mono">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
          <tr mat-header-row *matHeaderRowDef="colunas"></tr>
          <tr mat-row *matRowDef="let row; columns: colunas;" class="clickable-row"></tr>
        </table>
        <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
      </div>
    }
  `,
  styles: [`
    .table-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      overflow: hidden;
    }
    .mg-tag {
      display: inline-block;
      padding: 3px 10px;
      border-radius: var(--r-pill);
      background: var(--accent-tint);
      color: var(--accent-700);
      font-size: 12px;
      font-weight: 700;
    }
    .link {
      color: var(--bolt-700);
      font-weight: 600;
      text-decoration: none;
      &:hover { text-decoration: underline; }
    }
    .clickable-row { cursor: pointer; }
  `]
})
export class AnaliseMgComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  analises = signal<AnaliseMg[]>([]);
  total = signal(0);
  loading = signal(false);
  erro = signal<string | null>(null);
  page = 0;
  colunas = ['id', 'clienteId', 'ucId', 'status', 'createdAt'];

  constructor(private clienteService: ClienteService) {}
  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading.set(true);
    this.erro.set(null);
    this.clienteService.listarAnalisesMg(this.page, 20)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => { this.analises.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
        error: () => { this.erro.set('Erro ao carregar análises.'); this.loading.set(false); }
      });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }
}
