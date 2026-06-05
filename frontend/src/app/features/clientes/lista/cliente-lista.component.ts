import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ClienteService } from '../../../core/services/cliente.service';
import { ClienteResumo } from '../../../core/models/cliente.model';
import {
  DocumentoPipe, StatusBadgeComponent, EmptyStateComponent,
  SkeletonComponent, AvatarComponent,
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../shared';

@Component({
  selector: 'app-cliente-lista',
  standalone: true,
  imports: [
    CommonModule, RouterModule, FormsModule,
    MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule,
    MatButtonToggleModule, MatSnackBarModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatDialogModule,
    DocumentoPipe, StatusBadgeComponent, EmptyStateComponent,
    SkeletonComponent, AvatarComponent
  ],
  template: `
    <div class="page-header">
      <div class="header-left">
        <mat-form-field appearance="outline" class="search-field" subscriptSizing="dynamic">
          <mat-icon matPrefix>search</mat-icon>
          <input matInput placeholder="Buscar por nome ou documento..." [(ngModel)]="busca" (input)="onBusca()">
        </mat-form-field>
        <mat-button-toggle-group [(ngModel)]="filtroStatus" (change)="onFiltroChange()">
          <mat-button-toggle value="ativos">Ativos</mat-button-toggle>
          <mat-button-toggle value="inativos">Inativos</mat-button-toggle>
          <mat-button-toggle value="todos">Todos</mat-button-toggle>
        </mat-button-toggle-group>
        <mat-form-field appearance="outline" class="sort-field" subscriptSizing="dynamic">
          <mat-select [(ngModel)]="ordenacao" (selectionChange)="onFiltroChange()">
            <mat-option value="nome,asc">Nome A–Z</mat-option>
            <mat-option value="nome,desc">Nome Z–A</mat-option>
            <mat-option value="createdAt,desc">Mais recentes</mat-option>
            <mat-option value="createdAt,asc">Mais antigos</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
      <a mat-raised-button color="primary" routerLink="/clientes/novo">
        <mat-icon>add</mat-icon> Novo Cliente
      </a>
    </div>

    @if (loading()) {
      <div class="table-card">
        <div class="card-header"><h3>Clientes</h3></div>
        <app-skeleton [rowCount]="5" [columns]="[3, 2, 1, 2, 1]" />
      </div>
    } @else if (erro()) {
      <app-empty-state icon="error_outline" [message]="erro()!" />
    } @else if (clientesFiltrados().length === 0) {
      <app-empty-state icon="people_outline"
        [title]="busca ? 'Nenhum resultado' : 'Nenhum cliente'"
        [subtitle]="busca ? 'Tente outro termo de busca.' : 'Nenhum cliente encontrado com esse filtro.'" />
    } @else {
      <div class="table-card">
        <div class="card-header">
          <h3>Clientes <span class="count-badge">{{ total() }}</span></h3>
        </div>
        <table mat-table [dataSource]="clientesFiltrados()" class="full-width">
          <ng-container matColumnDef="nome">
            <th mat-header-cell *matHeaderCellDef>Cliente</th>
            <td mat-cell *matCellDef="let c">
              <div class="cell-cliente">
                <app-avatar [name]="c.nome" [size]="36" />
                <div class="cell-info">
                  <span class="cell-name">{{ c.nome }}</span>
                  <span class="cell-doc mono">{{ c.documento | documento }}</span>
                </div>
              </div>
            </td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let c">
              <app-status-badge [status]="c.ativo ? 'ativo' : 'inativo'" />
            </td>
          </ng-container>
          <ng-container matColumnDef="createdAt">
            <th mat-header-cell *matHeaderCellDef>Criado em</th>
            <td mat-cell *matCellDef="let c" class="mono">{{ c.createdAt | date:'dd/MM/yyyy' }}</td>
          </ng-container>
          <ng-container matColumnDef="acoes">
            <th mat-header-cell *matHeaderCellDef class="col-acoes">Ações</th>
            <td mat-cell *matCellDef="let c" class="col-acoes">
              <a mat-icon-button [routerLink]="['/clientes', c.id]" matTooltip="Ver detalhe">
                <mat-icon>visibility</mat-icon>
              </a>
              <a mat-icon-button [routerLink]="['/clientes', c.id, 'editar']" matTooltip="Editar" class="btn-soft">
                <mat-icon>edit</mat-icon>
              </a>
              @if (c.ativo) {
                <button mat-icon-button (click)="inativar(c)" matTooltip="Inativar" class="btn-danger">
                  <mat-icon>block</mat-icon>
                </button>
              }
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="colunas"></tr>
          <tr mat-row *matRowDef="let row; columns: colunas;" class="clickable-row"></tr>
        </table>
        <mat-paginator [length]="total()" [pageSize]="20" [pageIndex]="page" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
      </div>
    }
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      gap: 16px;
      flex-wrap: wrap;
    }
    .header-left {
      display: flex;
      align-items: center;
      gap: 16px;
      flex: 1;
      flex-wrap: wrap;
    }
    .search-field {
      max-width: 340px;
      flex: 1;
      min-width: 200px;
      mat-icon { color: var(--text-faint); margin-right: 4px; }
    }
    .sort-field {
      width: 170px;
      min-width: 150px;
    }

    .table-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      overflow: hidden;
    }

    .cell-cliente {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .cell-info {
      display: flex;
      flex-direction: column;
    }
    .cell-name {
      font-weight: 600;
      color: var(--text);
      font-size: 14px;
    }
    .cell-doc {
      font-size: 12.5px;
      color: var(--text-faint);
    }

    .col-acoes { text-align: right; }
    .clickable-row { cursor: pointer; }
  `]
})
export class ClienteListaComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  clientes = signal<ClienteResumo[]>([]);
  clientesFiltrados = signal<ClienteResumo[]>([]);
  total = signal(0);
  loading = signal(false);
  erro = signal<string | null>(null);
  page = 0;
  filtroStatus = 'ativos';
  ordenacao = 'nome,asc';
  busca = '';
  colunas = ['nome', 'status', 'createdAt', 'acoes'];

  constructor(
    private clienteService: ClienteService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.loading.set(true);
    this.erro.set(null);
    this.clienteService.listar(this.page, 20, this.filtroStatus, this.ordenacao)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => {
          this.clientes.set(p.content);
          this.total.set(p.totalElements);
          this.filtrar();
          this.loading.set(false);
        },
        error: () => { this.erro.set('Erro ao carregar clientes.'); this.loading.set(false); }
      });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }

  onFiltroChange() { this.page = 0; this.carregar(); }

  onBusca() { this.filtrar(); }

  private filtrar() {
    const termo = this.busca.toLowerCase();
    const termoDigitos = this.busca.replace(/\D/g, '');
    if (!termo) {
      this.clientesFiltrados.set(this.clientes());
      return;
    }
    this.clientesFiltrados.set(
      this.clientes().filter(c =>
        c.nome.toLowerCase().includes(termo) ||
        (termoDigitos && c.documento.includes(termoDigitos))
      )
    );
  }

  inativar(c: ClienteResumo) {
    const data: ConfirmDialogData = {
      title: 'Inativar cliente',
      message: `Deseja inativar "${c.nome}"? O cliente ficará inativo mas seus dados serão preservados.`,
      icon: 'block',
      confirmLabel: 'Inativar',
      variant: 'danger'
    };
    this.dialog.open(ConfirmDialogComponent, { data, width: '420px' })
      .afterClosed().subscribe(confirmed => {
        if (!confirmed) return;
        this.clienteService.inativar(c.id).subscribe({
          next: () => { this.snackBar.open('Cliente inativado com sucesso', 'OK', { duration: 4000 }); this.carregar(); },
          error: () => this.snackBar.open('Erro ao inativar cliente', 'OK', { duration: 4000 })
        });
      });
  }
}
