import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ClienteService } from '../../../core/services/cliente.service';
import { UnidadeConsumidora, UnidadeConsumidoraRequest } from '../../../core/models/cliente.model';
import { EnderecoFormComponent } from '../../../shared';

@Component({
  selector: 'app-uc-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatProgressSpinnerModule, EnderecoFormComponent
  ],
  template: `
    <div class="dialog-header">
      <div class="dialog-chip"><mat-icon>business</mat-icon></div>
      <h2 mat-dialog-title>{{ editando ? 'Editar' : 'Nova' }} Unidade Consumidora</h2>
    </div>
    <mat-dialog-content>
      <form [formGroup]="form" class="uc-form">
        <div class="row">
          <mat-form-field appearance="outline" class="flex-2">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome" placeholder="Ex: Sede, Filial Centro">
          </mat-form-field>
          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Nº Instalação</mat-label>
            <input matInput formControlName="numeroInstalacao">
          </mat-form-field>
        </div>
        <h4 class="subsection">Endereço</h4>
        <app-endereco-form [form]="enderecoGroup" />
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close class="btn-ghost">Cancelar</button>
      <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="loading()">
        @if (loading()) { <mat-spinner diameter="20"></mat-spinner> } @else { Salvar }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 20px 24px 0;
    }
    .dialog-chip {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      background: var(--bolt-500);
      display: flex;
      align-items: center;
      justify-content: center;
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--text-onbrand); }
    }
    .uc-form { min-width: 500px; padding-top: 4px; }
    .subsection {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-soft);
      margin: 12px 0 8px;
    }
    .btn-ghost {
      border: 1px solid var(--border-strong) !important;
      border-radius: var(--r-pill) !important;
      color: var(--text) !important;
    }
  `]
})
export class UcFormComponent implements OnInit {
  form!: FormGroup;
  editando = false;
  loading = signal(false);

  get enderecoGroup() { return this.form.get('endereco') as FormGroup; }

  constructor(
    private fb: FormBuilder,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar,
    private dialogRef: MatDialogRef<UcFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: { clienteId: number; uc?: UnidadeConsumidora }
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      nome: ['', Validators.required],
      numeroInstalacao: ['', Validators.required],
      endereco: EnderecoFormComponent.createGroup(this.fb)
    });

    if (this.data.uc) {
      this.editando = true;
      this.form.patchValue({ nome: this.data.uc.nome, numeroInstalacao: this.data.uc.numeroInstalacao, endereco: this.data.uc.endereco });
    }
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const val = this.form.value;
    const request: UnidadeConsumidoraRequest = {
      nome: val.nome, numeroInstalacao: val.numeroInstalacao,
      endereco: { cep: val.endereco.cep, numero: val.endereco.numero, complemento: val.endereco.complemento }
    };

    const obs = this.editando
      ? this.clienteService.atualizarUC(this.data.clienteId, this.data.uc!.id, request)
      : this.clienteService.adicionarUC(this.data.clienteId, request);

    obs.subscribe({
      next: () => { this.loading.set(false); this.snackBar.open(this.editando ? 'UC atualizada!' : 'UC adicionada!', 'OK', { duration: 3000 }); this.dialogRef.close(true); },
      error: (err) => { this.loading.set(false); this.snackBar.open(err.error?.detail || 'Erro ao salvar UC', 'OK', { duration: 5000 }); }
    });
  }
}
