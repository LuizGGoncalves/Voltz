import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
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
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
    MatSnackBarModule, MatProgressSpinnerModule, NgxMaskDirective,
    EnderecoFormComponent
  ],
  template: `
    <div class="form-page">
      <form [formGroup]="form" (ngSubmit)="onSubmit()">
        <!-- Seção: Dados do Cliente -->
        <section class="form-section">
          <div class="section-header">
            <div class="section-chip"><mat-icon>person</mat-icon></div>
            <h3>Dados do Cliente</h3>
          </div>
          <div class="section-body">
            <div class="row">
              <mat-form-field appearance="outline" class="flex-2">
                <mat-label>Nome</mat-label>
                <input matInput formControlName="nome" placeholder="Nome completo ou razão social">
              </mat-form-field>
              <mat-form-field appearance="outline" class="flex-1">
                <mat-label>CPF/CNPJ</mat-label>
                <input matInput formControlName="documento" [mask]="docMask" [dropSpecialCharacters]="false"
                       placeholder="Digite o documento">
              </mat-form-field>
            </div>
          </div>
        </section>

        <!-- Seção: Endereço -->
        <section class="form-section">
          <div class="section-header">
            <div class="section-chip accent"><mat-icon>location_on</mat-icon></div>
            <h3>Endereço</h3>
          </div>
          <div class="section-body">
            <app-endereco-form [form]="enderecoGroup" />
          </div>
        </section>

        <!-- Seção: UC Inicial (só na criação) -->
        @if (!editando) {
          <section class="form-section">
            <div class="section-header">
              <div class="section-chip info"><mat-icon>business</mat-icon></div>
              <h3>Unidade Consumidora Inicial</h3>
            </div>
            <div class="section-body" formGroupName="uc">
              <div class="row">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>Nome da UC</mat-label>
                  <input matInput formControlName="nome" placeholder="Ex: Sede, Filial Centro">
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Nº Instalação</mat-label>
                  <input matInput formControlName="numeroInstalacao">
                </mat-form-field>
              </div>
              <app-endereco-form [form]="ucEnderecoGroup" />
              <p class="hint">Após criar, gerencie as UCs na tela de detalhe do cliente.</p>
            </div>
          </section>
        }

        <!-- Barra de ações fixa -->
        <div class="form-actions-bar">
          <div class="form-actions-inner">
            <button mat-button type="button" (click)="voltar()" class="btn-ghost">
              <mat-icon>arrow_back</mat-icon> Cancelar
            </button>
            <button mat-raised-button color="primary" type="submit" [disabled]="loading()">
              @if (loading()) { <mat-spinner diameter="20"></mat-spinner> } @else { Salvar }
            </button>
          </div>
        </div>
      </form>
    </div>
  `,
  styles: [`
    .form-page {
      max-width: 920px;
    }

    .form-section {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--r-lg);
      box-shadow: var(--sh-1);
      margin-bottom: 20px;
      overflow: hidden;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px 22px;
      border-bottom: 1px solid var(--border);
    }

    .section-chip {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      background: var(--bolt-500);
      display: flex;
      align-items: center;
      justify-content: center;
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--text-onbrand); }

      &.accent { background: var(--accent-600); }
      &.info { background: var(--info-fg); }
    }

    .section-body {
      padding: 20px 22px;
    }

    .hint {
      font-size: 12.5px;
      font-weight: 500;
      color: var(--text-faint);
      margin: 8px 0 0;
    }

    .btn-ghost {
      border: 1px solid var(--border-strong) !important;
      border-radius: var(--r-pill) !important;
      color: var(--text) !important;
      mat-icon { font-size: 18px; margin-right: 4px; }
    }

    .form-actions-bar {
      position: sticky;
      bottom: 0;
      background: var(--surface);
      border-top: 1px solid var(--border);
      margin: 0 -28px -26px;
      padding: 14px 28px;
      z-index: 10;
    }

    .form-actions-inner {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      max-width: 920px;
    }
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
