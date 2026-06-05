import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../../core/services/cliente.service';
import { ClienteResumo } from '../../../core/models/cliente.model';
import { DocumentoPipe, StatusBadgeComponent, EmptyStateComponent } from '../../../shared';

@Component({
  selector: 'app-cliente-lista',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatSlideToggleModule, MatSnackBarModule, MatTooltipModule, MatProgressSpinnerModule,
    DocumentoPipe, StatusBadgeComponent, EmptyStateComponent
  ],
  template: `
    <div class="header">
      <h2>Clientes</h2>
      <div class="actions">
        <mat-slide-toggle [(ngModel)]="incluirInativos" (change)="carregar()">Incluir inativos</mat-slide-toggle>
        <a mat-raised-button color="primary" routerLink="/clientes/novo">
          <mat-icon>add</mat-icon> Novo Cliente
        </a>
      </div>
    </div>

    @if (loading()) {
      <div class="center"><mat-spinner diameter="40"></mat-spinner></div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (clientes().length === 0) {
      <app-empty-state icon="people_outline" message="Nenhum cliente encontrado. Crie o primeiro!" />
    } @else {
      <table mat-table [dataSource]="clientes()" class="full-width">
        <ng-container matColumnDef="nome">
          <th mat-header-cell *matHeaderCellDef>Nome</th>
          <td mat-cell *matCellDef="let c">{{ c.nome }}</td>
        </ng-container>
        <ng-container matColumnDef="documento">
          <th mat-header-cell *matHeaderCellDef>Documento</th>
          <td mat-cell *matCellDef="let c">{{ c.documento | documento }}</td>
        </ng-container>
        <ng-container matColumnDef="status">
          <th mat-header-cell *matHeaderCellDef>Status</th>
          <td mat-cell *matCellDef="let c">
            <app-status-badge [status]="c.ativo ? 'ativo' : 'inativo'" [label]="c.ativo ? 'Ativo' : 'Inativo'" />
          </td>
        </ng-container>
        <ng-container matColumnDef="createdAt">
          <th mat-header-cell *matHeaderCellDef>Criado em</th>
          <td mat-cell *matCellDef="let c">{{ c.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
        </ng-container>
        <ng-container matColumnDef="acoes">
          <th mat-header-cell *matHeaderCellDef>Ações</th>
          <td mat-cell *matCellDef="let c">
            <a mat-icon-button [routerLink]="['/clientes', c.id]" aria-label="Ver detalhe"><mat-icon>visibility</mat-icon></a>
            <a mat-icon-button [routerLink]="['/clientes', c.id, 'editar']" aria-label="Editar"><mat-icon>edit</mat-icon></a>
            @if (c.ativo) {
              <button mat-icon-button color="warn" (click)="inativar(c)" aria-label="Inativar"><mat-icon>delete</mat-icon></button>
            }
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="colunas"></tr>
        <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
      </table>
      <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
    }
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .actions { display: flex; gap: 16px; align-items: center; }
    .full-width { width: 100%; }
    .center { display: flex; justify-content: center; padding: 48px; }
  `]
})
export class ClienteListaComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  clientes = signal<ClienteResumo[]>([]);
  total = signal(0);
  loading = signal(false);
  erro = signal<string | null>(null);
  page = 0;
  incluirInativos = false;
  colunas = ['nome', 'documento', 'status', 'createdAt', 'acoes'];

  constructor(private clienteService: ClienteService, private snackBar: MatSnackBar) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading.set(true);
    this.erro.set(null);
    this.clienteService.listar(this.page, 20, this.incluirInativos)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => { this.clientes.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
        error: () => { this.erro.set('Erro ao carregar clientes.'); this.loading.set(false); }
      });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }

  inativar(c: ClienteResumo) {
    if (!confirm(`Inativar "${c.nome}"?`)) return;
    this.clienteService.inativar(c.id).subscribe({
      next: () => { this.snackBar.open('Cliente inativado', 'OK', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao inativar', 'OK', { duration: 3000 })
    });
  }
}
