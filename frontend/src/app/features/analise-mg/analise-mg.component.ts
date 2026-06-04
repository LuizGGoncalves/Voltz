import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatChipsModule } from '@angular/material/chips';
import { ClienteService } from '../../core/services/cliente.service';
import { AnaliseMg } from '../../core/models/cliente.model';

@Component({
  selector: 'app-analise-mg',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatPaginatorModule, MatChipsModule],
  template: `
    <h2>Análises MG</h2>
    <p class="subtitle">Clientes com unidades consumidoras em Minas Gerais registrados para análise.</p>
    <table mat-table [dataSource]="analises()" class="full-width">
      <ng-container matColumnDef="id"><th mat-header-cell *matHeaderCellDef>ID</th><td mat-cell *matCellDef="let a">{{ a.id }}</td></ng-container>
      <ng-container matColumnDef="clienteId"><th mat-header-cell *matHeaderCellDef>Cliente ID</th><td mat-cell *matCellDef="let a">{{ a.clienteId }}</td></ng-container>
      <ng-container matColumnDef="ucId"><th mat-header-cell *matHeaderCellDef>UC ID</th><td mat-cell *matCellDef="let a">{{ a.unidadeConsumidoraId }}</td></ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let a"><span class="badge">{{ a.status }}</span></td>
      </ng-container>
      <ng-container matColumnDef="createdAt"><th mat-header-cell *matHeaderCellDef>Registrado em</th><td mat-cell *matCellDef="let a">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</td></ng-container>
      <tr mat-header-row *matHeaderRowDef="colunas"></tr>
      <tr mat-row *matRowDef="let row; columns: colunas;"></tr>
    </table>
    <mat-paginator [length]="total()" [pageSize]="20" (page)="onPage($event)" showFirstLastButtons></mat-paginator>
  `,
  styles: [`
    .full-width { width: 100%; }
    .subtitle { color: #666; margin-bottom: 16px; }
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; background: #fff3e0; color: #e65100; }
  `]
})
export class AnaliseMgComponent implements OnInit {
  analises = signal<AnaliseMg[]>([]);
  total = signal(0);
  page = 0;
  colunas = ['id', 'clienteId', 'ucId', 'status', 'createdAt'];

  constructor(private clienteService: ClienteService) {}
  ngOnInit() { this.carregar(); }

  carregar() {
    this.clienteService.listarAnalisesMg(this.page, 20).subscribe(p => {
      this.analises.set(p.content);
      this.total.set(p.totalElements);
    });
  }

  onPage(e: PageEvent) { this.page = e.pageIndex; this.carregar(); }
}
