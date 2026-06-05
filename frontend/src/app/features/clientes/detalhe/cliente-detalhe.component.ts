import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { ClienteService } from '../../../core/services/cliente.service';
import { Cliente, UnidadeConsumidora } from '../../../core/models/cliente.model';
import { UcFormComponent } from '../uc-form/uc-form.component';

@Component({
  selector: 'app-cliente-detalhe',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule, MatIconModule, MatTableModule, MatChipsModule, MatSnackBarModule, MatDialogModule],
  template: `
    @if (cliente()) {
      <div class="header">
        <h2>{{ cliente()!.nome }}</h2>
        <div class="actions">
          <a mat-stroked-button [routerLink]="['/clientes', cliente()!.id, 'editar']"><mat-icon>edit</mat-icon> Editar Cliente</a>
          <a mat-button routerLink="/clientes">Voltar</a>
        </div>
      </div>

      <mat-card>
        <mat-card-content>
          <div class="info-grid">
            <div><strong>Documento:</strong> {{ cliente()!.documento }} ({{ cliente()!.tipoDocumento }})</div>
            <div><strong>Status:</strong> <span class="badge" [class.ativo]="cliente()!.ativo" [class.inativo]="!cliente()!.ativo">{{ cliente()!.ativo ? 'Ativo' : 'Inativo' }}</span></div>
            <div><strong>Endereço:</strong> {{ cliente()!.endereco.logradouro }}, {{ cliente()!.endereco.numero }} — {{ cliente()!.endereco.cidade }}/{{ cliente()!.endereco.uf }}</div>
          </div>
        </mat-card-content>
      </mat-card>

      <div class="uc-header">
        <h3>Unidades Consumidoras</h3>
        <button mat-raised-button color="primary" (click)="abrirFormUC()"><mat-icon>add</mat-icon> Nova UC</button>
      </div>

      <table mat-table [dataSource]="ucs()" class="full-width">
        <ng-container matColumnDef="nome"><th mat-header-cell *matHeaderCellDef>Nome</th><td mat-cell *matCellDef="let uc">{{ uc.nome }}</td></ng-container>
        <ng-container matColumnDef="instalacao"><th mat-header-cell *matHeaderCellDef>Nº Instalação</th><td mat-cell *matCellDef="let uc">{{ uc.numeroInstalacao }}</td></ng-container>
        <ng-container matColumnDef="endereco"><th mat-header-cell *matHeaderCellDef>Endereço</th><td mat-cell *matCellDef="let uc">{{ uc.endereco.cidade }}/{{ uc.endereco.uf }}</td></ng-container>
        <ng-container matColumnDef="acoes">
          <th mat-header-cell *matHeaderCellDef>Ações</th>
          <td mat-cell *matCellDef="let uc">
            <button mat-icon-button (click)="abrirFormUC(uc)"><mat-icon>edit</mat-icon></button>
            <button mat-icon-button color="warn" (click)="removerUC(uc)"><mat-icon>delete</mat-icon></button>
          </td>
        </ng-container>
        <tr mat-header-row *matHeaderRowDef="['nome', 'instalacao', 'endereco', 'acoes']"></tr>
        <tr mat-row *matRowDef="let row; columns: ['nome', 'instalacao', 'endereco', 'acoes'];"></tr>
      </table>
    }
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    .actions { display: flex; gap: 8px; }
    .info-grid { display: flex; flex-direction: column; gap: 8px; }
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
    .ativo { background: #e8f5e9; color: #2e7d32; }
    .inativo { background: #fce4ec; color: #c62828; }
    .uc-header { display: flex; justify-content: space-between; align-items: center; margin: 24px 0 16px; }
    .full-width { width: 100%; }
  `]
})
export class ClienteDetalheComponent implements OnInit {
  cliente = signal<Cliente | null>(null);
  ucs = signal<UnidadeConsumidora[]>([]);
  clienteId!: number;

  constructor(
    private route: ActivatedRoute,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit() {
    this.clienteId = +this.route.snapshot.paramMap.get('id')!;
    this.carregar();
  }

  carregar() {
    this.clienteService.buscarPorId(this.clienteId).subscribe(c => this.cliente.set(c));
    this.clienteService.listarUCs(this.clienteId).subscribe(ucs => this.ucs.set(ucs));
  }

  abrirFormUC(uc?: UnidadeConsumidora) {
    const ref = this.dialog.open(UcFormComponent, {
      width: '600px',
      data: { clienteId: this.clienteId, uc }
    });
    ref.afterClosed().subscribe(result => { if (result) this.carregar(); });
  }

  removerUC(uc: UnidadeConsumidora) {
    if (!confirm(`Remover "${uc.nome}"?`)) return;
    this.clienteService.removerUC(this.clienteId, uc.id).subscribe({
      next: () => { this.snackBar.open('UC removida', 'OK', { duration: 3000 }); this.carregar(); },
      error: () => this.snackBar.open('Erro ao remover', 'OK', { duration: 3000 })
    });
  }
}
