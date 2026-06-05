import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../../../core/services/cliente.service';
import { ClienteResumo } from '../../../core/models/cliente.model';

@Component({
  selector: 'app-cliente-lista',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, MatTableModule, MatPaginatorModule, MatButtonModule, MatIconModule, MatChipsModule, MatSlideToggleModule, MatSnackBarModule, MatTooltipModule],
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
    <table mat-table [dataSource]="clientes()" class="full-width">
      <ng-container matColumnDef="nome">
        <th mat-header-cell *matHeaderCellDef>Nome</th>
        <td mat-cell *matCellDef="let c">{{ c.nome }}</td>
      </ng-container>
      <ng-container matColumnDef="documento">
        <th mat-header-cell *matHeaderCellDef>Documento</th>
        <td mat-cell *matCellDef="let c">{{ formatDoc(c.documento, c.tipoDocumento) }}</td>
      </ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let c">
          <span class="badge" [class.ativo]="c.ativo" [class.inativo]="!c.ativo">
            {{ c.ativo ? 'Ativo' : 'Inativo' }}
          </span>
        </td>
      </ng-container>
      <ng-container matColumnDef="createdAt">
        <th mat-header-cell *matHeaderCellDef>Criado em</th>
        <td mat-cell *matCellDef="let c">{{ c.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
      </ng-container>
      <ng-container matColumnDef="acoes">
        <th mat-header-cell *matHeaderCellDef>Ações</th>
        <td mat-cell *matCellDef="let c">
          <a mat-icon-button [routerLink]="['/clientes', c.id]" matTooltip="Ver detalhe"><mat-icon>visibility</mat-icon></a>
          <a mat-icon-button [routerLink]="['/clientes', c.id, 'editar']" matTooltip="Editar"><mat-icon>edit</mat-icon></a>
          @if (c.ativo) {
            <button mat-icon-button color="warn" (click)="inativar(c)"><mat-icon>delete</mat-icon></button>
          }
        </td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="colunas"></tr>
      <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
    </table>
    <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .actions { display: flex; gap: 16px; align-items: center; }
    .full-width { width: 100%; }
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .ativo { background: #e8f5e9; color: #2e7d32; }
    .inativo { background: #fce4ec; color: #c62828; }
  `]
})
export class ClienteListaComponent implements OnInit {
  clientes = signal<ClienteResumo[]>([]);
  total = signal(0);
  page = 0;
  incluirInativos = false;
  colunas = ['nome', 'documento', 'status', 'createdAt', 'acoes'];

  constructor(private clienteService: ClienteService, private snackBar: MatSnackBar) {}

  ngOnInit() { this.carregar(); }

  carregar() {
    this.clienteService.listar(this.page, 20, this.incluirInativos).subscribe(p => {
      this.clientes.set(p.content);
      this.total.set(p.totalElements);
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

  formatDoc(doc: string, tipo: string): string {
    if (tipo === 'CPF' && doc.length === 11) return `${doc.slice(0,3)}.${doc.slice(3,6)}.${doc.slice(6,9)}-${doc.slice(9)}`;
    if (tipo === 'CNPJ' && doc.length === 14) return `${doc.slice(0,2)}.${doc.slice(2,5)}.${doc.slice(5,8)}/${doc.slice(8,12)}-${doc.slice(12)}`;
    return doc;
  }
}
