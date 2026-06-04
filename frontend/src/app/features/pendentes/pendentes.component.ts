import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { FormsModule } from '@angular/forms';
import { ClienteService } from '../../core/services/cliente.service';
import { CadastroPendente } from '../../core/models/cliente.model';

@Component({
  selector: 'app-pendentes',
  standalone: true,
  imports: [CommonModule, FormsModule, MatTableModule, MatPaginatorModule, MatChipsModule, MatButtonToggleModule],
  template: `
    <h2>Cadastros Pendentes</h2>
    <mat-button-toggle-group [(ngModel)]="filtroStatus" (change)="carregar()">
      <mat-button-toggle value="">Todos</mat-button-toggle>
      <mat-button-toggle value="PENDENTE">Pendente</mat-button-toggle>
      <mat-button-toggle value="PROCESSADO">Processado</mat-button-toggle>
      <mat-button-toggle value="REJEITADO">Rejeitado</mat-button-toggle>
      <mat-button-toggle value="FALHA">Falha</mat-button-toggle>
    </mat-button-toggle-group>
    <table mat-table [dataSource]="pendentes()" class="full-width" style="margin-top:16px">
      <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let p">{{ p.id }}</td></ng-container>
      <ng-container matColumnDef="documento"><th mat-header-cell *matHeaderCellDef>Documento</th><td mat-cell *matCellDef="let p">{{ p.documento }}</td></ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let p">
          <span class="badge" [ngClass]="p.status.toLowerCase()">{{ p.status }}</span>
        </td>
      </ng-container>
      <ng-container matColumnDef="motivo"><th mat-header-cell *matHeaderCellDef>Motivo</th><td mat-cell *matCellDef="let p">{{ p.motivo || '-' }}</td></ng-container>
      <ng-container matColumnDef="tentativas"><th mat-header-cell *matHeaderCellDef>Tentativas</th><td mat-cell *matCellDef="let p">{{ p.tentativas }}</td></ng-container>
      <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Criado em</th><td mat-cell *matCellDef="let p">{{ p.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
      <tr mat-header-row *matHeaderRowDef="colunas"></tr>
      <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
    </table>
    <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
  `,
  styles: [`
    .full-width { width: 100%; }
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .pendente { background: #fff3e0; color: #e65100; }
    .processado { background: #e8f5e9; color: #2e7d32; }
    .rejeitado { background: #fce4ec; color: #c62828; }
    .falha { background: #ffebee; color: #b71c1c; }
  `]
})
export class PendentesComponent implements OnInit {
  pendentes = signal<CadastroPendente[]>([]);
  total = signal(0);
  page = 0;
  filtroStatus = '';
  colunas = ['id', 'documento', 'status', 'motivo', 'tentativas', 'createdAt'];

  constructor(private clienteService: ClienteService) {}
  ngOnInit() { this.carregar(); }

  carregar() {
    this.clienteService.listarPendentes(this.page, 20, this.filtroStatus || undefined).subscribe(p => {
      this.pendentes.set(p.content);
      this.total.set(p.totalElements);
    });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }
}
