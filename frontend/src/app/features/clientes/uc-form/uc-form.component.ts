import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
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
    MatFormFieldModule, MatInputModule, MatButtonModule,
    MatSnackBarModule, MatProgressSpinnerModule, EnderecoFormComponent
  ],
  template: `
    <h2 mat-dialog-title>{{ editando ? 'Editar' : 'Nova' }} Unidade Consumidora</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="uc-form">
        <div class="row">
          <mat-form-field appearance="outline" class="flex-2">
            <mat-label>Nome</mat-label>
            <input matInput formControlName="nome">
          </mat-form-field>
          <mat-form-field appearance="outline" class="flex-1">
            <mat-label>Nº Instalação</mat-label>
            <input matInput formControlName="numeroInstalacao">
          </mat-form-field>
        </div>
        <h4>Endereço</h4>
        <app-endereco-form [form]="enderecoGroup" />
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancelar</button>
      <button mat-raised-button color="primary" (click)="onSubmit()" [disabled]="loading()">
        @if (loading()) { <mat-spinner diameter="20"></mat-spinner> } @else { Salvar }
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .uc-form { min-width: 500px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .flex-1 { flex: 1; min-width: 130px; }
    .flex-2 { flex: 2; min-width: 200px; }
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
