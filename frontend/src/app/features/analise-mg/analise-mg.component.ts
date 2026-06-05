import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../core/services/cliente.service';
import { AnaliseMg } from '../../core/models/cliente.model';
import { StatusBadgeComponent, EmptyStateComponent } from '../../shared';

@Component({
  selector: 'app-analise-mg',
  standalone: true,
  imports: [
    CommonModule, MatTableModule, MatPaginatorModule, MatProgressSpinnerModule,
    StatusBadgeComponent, EmptyStateComponent
  ],
  template: `
    <h2>Análises MG</h2>
    <p class="subtitle">Clientes com unidades consumidoras em Minas Gerais registrados para análise.</p>

    @if (loading()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (analises().length === 0) {
      <app-empty-state icon="assignment" message="Nenhuma análise MG registrada." />
    } @else {
      <table mat-table [dataSource]="analises()" class="full-width">
        <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let a">{{ a.id }}</td></ng-container>
        <ng-container matColumnDef="clienteId"><th mat-header-cell *matHeaderCellDef>Cliente ID</th><td mat-cell *matCellDef="let a">{{ a.clienteId }}</td></ng-container>
        <ng-container matColumnDef="ucId"><th mat-header-cell *matHeaderCellDef>UC ID</th><td mat-cell *matCellDef="let a">{{ a.unidadeConsumidoraId }}</td></ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let a"><app-status-badge [status]="a.status" [label]="a.status" /></td>
        </ng-container>
        <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Registrado em</th><td mat-cell *matCellDef="let a">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
        <tr mat-header-row *matHeaderRowDef="colunas"></tr>
        <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
      </table>
      <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
    }
  `,
  styles: [`.full-width { width: 100%; } .subtitle { color: #666; margin-bottom: 16px; } .center { display: flex; justify-content: center; padding: 48px; }`]
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
