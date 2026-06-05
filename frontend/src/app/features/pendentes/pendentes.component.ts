import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../core/services/cliente.service';
import { CadastroPendente } from '../../core/models/cliente.model';
import { StatusBadgeComponent, EmptyStateComponent, DocumentoPipe } from '../../shared';

@Component({
  selector: 'app-pendentes',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTableModule, MatPaginatorModule, MatButtonToggleModule, MatProgressSpinnerModule,
    StatusBadgeComponent, EmptyStateComponent, DocumentoPipe
  ],
  template: `
    <h2>Cadastros Pendentes</h2>
    <mat-button-toggle-group [(ngModel)]="filtroStatus" (change)="carregar()">
      <mat-button-toggle value="">Todos</mat-button-toggle>
      <mat-button-toggle value="PENDENTE">Pendente</mat-button-toggle>
      <mat-button-toggle value="PROCESSADO">Processado</mat-button-toggle>
      <mat-button-toggle value="REJEITADO">Rejeitado</mat-button-toggle>
      <mat-button-toggle value="FALHA">Falha</mat-button-toggle>
    </mat-button-toggle-group>

    @if (loading()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (pendentes().length === 0) {
      <app-empty-state icon="hourglass_empty" message="Nenhum cadastro pendente." />
    } @else {
      <table mat-table [dataSource]="pendentes()" class="full-width" style="margin-top:16px">
        <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let p">{{ p.id }}</td></ng-container>
        <ng-container matColumnDef="documento"><th mat-header-cell *matHeaderCellDef>Documento</th><td mat-cell *matCellDef="let p">{{ p.documento | documento }}</td></ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let p"><app-status-badge [status]="p.status" [label]="p.status" /></td>
        </ng-container>
        <ng-container matColumnDef="motivo"><th mat-header-cell *matHeaderCellDef>Motivo</th><td mat-cell *matCellDef="let p">{{ p.motivo || '-' }}</td></ng-container>
        <ng-container matColumnDef="tentativas"><th mat-header-cell *matHeaderCellDef>Tentativas</th><td mat-cell *matCellDef="let p">{{ p.tentativas }}</td></ng-container>
        <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Criado em</th><td mat-cell *matCellDef="let p">{{ p.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
        <tr mat-header-row *matHeaderRowDef="colunas"></tr>
        <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
      </table>
      <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
    }
  `,
  styles: [`.full-width { width: 100%; } .center { display: flex; justify-content: center; padding: 48px; }`]
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
