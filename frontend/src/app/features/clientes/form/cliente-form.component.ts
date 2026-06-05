import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgxMaskDirective } from 'ngx-mask';
import { ClienteService } from '../../../core/services/cliente.service';
import { ClienteRequest, ClienteUpdateRequest } from '../../../core/models/cliente.model';
import { EnderecoFormComponent } from '../../../shared';

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatCardModule,
    MatSnackBarModule, MatProgressSpinnerModule, NgxMaskDirective,
    EnderecoFormComponent
  ],
  template: `
    <h2>{{ editando ? 'Editar' : 'Novo' }} Cliente</h2>
    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <mat-card>
        <mat-card-content>
          <h3>Dados do Cliente</h3>
          <div class="row">
            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>Nome</mat-label>
              <input matInput formControlName="nome">
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>CPF/CNPJ</mat-label>
              <input matInput formControlName="documento" [mask]="docMask" [dropSpecialCharacters]="false">
            </mat-form-field>
          </div>
          <h4>Endereço</h4>
          <app-endereco-form [form]="enderecoGroup" />

          @if (!editando) {
            <h3 style="margin-top:16px">Unidade Consumidora Inicial</h3>
            <div class="row" formGroupName="uc">
              <mat-form-field appearance="outline" class="flex-2">
                <mat-label>Nome da UC</mat-label>
                <input matInput formControlName="nome">
              </mat-form-field>
              <mat-form-field appearance="outline" class="flex-1">
                <mat-label>Nº Instalação</mat-label>
                <input matInput formControlName="numeroInstalacao">
              </mat-form-field>
            </div>
            <app-endereco-form [form]="ucEnderecoGroup" />
            <p class="hint">Após criar, gerencie as UCs na tela de detalhe do cliente.</p>
          }
        </mat-card-content>
      </mat-card>

      <div class="form-actions">
        <button mat-button type="button" (click)="voltar()">Cancelar</button>
        <button mat-raised-button color="primary" type="submit" [disabled]="loading()">
          @if (loading()) { <mat-spinner diameter="20"></mat-spinner> } @else { Salvar }
        </button>
      </div>
    </form>
  `,
  styles: [`
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .flex-1 { flex: 1; min-width: 150px; }
    .flex-2 { flex: 2; min-width: 200px; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
    .hint { color: #888; font-size: 13px; margin-top: 8px; }
  `]
})
export class ClienteFormComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  form!: FormGroup;
  editando = false;
  clienteId?: number;
  loading = signal(false);
  docMask = '000.000.000-009';

  constructor(
    private fb: FormBuilder,
    private clienteService: ClienteService,
    private snackBar: MatSnackBar,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  get enderecoGroup() { return this.form.get('endereco') as FormGroup; }
  get ucEnderecoGroup() { return this.form.get('uc.endereco') as FormGroup; }

  ngOnInit() {
    this.form = this.fb.group({
      nome: ['', Validators.required],
      documento: ['', Validators.required],
      endereco: EnderecoFormComponent.createGroup(this.fb),
      uc: this.fb.group({
        nome: ['', Validators.required],
        numeroInstalacao: ['', Validators.required],
        endereco: EnderecoFormComponent.createGroup(this.fb)
      })
    });

    this.form.get('documento')?.valueChanges.subscribe(v => {
      const digits = v?.replace(/\D/g, '') || '';
      this.docMask = digits.length > 11 ? '00.000.000/0000-00' : '000.000.000-009';
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editando = true;
      this.clienteId = +id;
      this.form.removeControl('uc');
      this.clienteService.buscarPorId(this.clienteId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe(c => this.form.patchValue({ nome: c.nome, documento: c.documento, endereco: c.endereco }));
    }
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const val = this.form.value;
    const endereco = { cep: val.endereco.cep, numero: val.endereco.numero, complemento: val.endereco.complemento };

    if (this.editando) {
      const request: ClienteUpdateRequest = { nome: val.nome, documento: val.documento, endereco };
      this.clienteService.atualizar(this.clienteId!, request).subscribe({
        next: () => { this.loading.set(false); this.snackBar.open('Cliente atualizado!', 'OK', { duration: 3000 }); this.router.navigate(['/clientes', this.clienteId]); },
        error: (err) => { this.loading.set(false); this.snackBar.open(err.error?.detail || 'Erro ao salvar', 'OK', { duration: 5000 }); }
      });
    } else {
      const ucVal = val.uc;
      const request: ClienteRequest = {
        nome: val.nome, documento: val.documento, endereco,
        unidadesConsumidoras: [{
          nome: ucVal.nome, numeroInstalacao: ucVal.numeroInstalacao,
          endereco: { cep: ucVal.endereco.cep, numero: ucVal.endereco.numero, complemento: ucVal.endereco.complemento }
        }]
      };
      this.clienteService.criar(request).subscribe({
        next: (res: any) => {
          this.loading.set(false);
          this.snackBar.open(res.cadastroPendenteId ? `Cadastro em processamento (ID: ${res.cadastroPendenteId})` : 'Cliente criado!', 'OK', { duration: 3000 });
          this.router.navigate(['/clientes']);
        },
        error: (err) => { this.loading.set(false); this.snackBar.open(err.error?.detail || 'Erro ao salvar', 'OK', { duration: 5000 }); }
      });
    }
  }

  voltar() { this.router.navigate(['/clientes']); }
}
