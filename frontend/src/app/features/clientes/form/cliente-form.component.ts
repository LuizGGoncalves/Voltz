import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, FormGroup, FormArray, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgxMaskDirective } from 'ngx-mask';
import { ClienteService } from '../../../core/services/cliente.service';
import { ViaCepService } from '../../../core/services/viacep.service';
import { ClienteRequest, ClienteUpdateRequest, CadastroPendenteCreated, UnidadeConsumidoraRequest } from '../../../core/models/cliente.model';

@Component({
  selector: 'app-cliente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatCardModule, MatSnackBarModule, MatProgressSpinnerModule, NgxMaskDirective],
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
              <input matInput formControlName="documento" [mask]="docMask()" [dropSpecialCharacters]="false">
            </mat-form-field>
          </div>
          <h4>Endereço</h4>
          <div formGroupName="endereco" class="row">
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>CEP</mat-label>
              <input matInput formControlName="cep" mask="00000-000" (blur)="buscarCep()">
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Número</mat-label>
              <input matInput formControlName="numero">
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Complemento</mat-label>
              <input matInput formControlName="complemento">
            </mat-form-field>
          </div>
          <div formGroupName="endereco" class="row readonly-fields">
            <mat-form-field appearance="outline" class="flex-2">
              <mat-label>Logradouro</mat-label>
              <input matInput formControlName="logradouro" readonly>
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Bairro</mat-label>
              <input matInput formControlName="bairro" readonly>
            </mat-form-field>
            <mat-form-field appearance="outline" class="flex-1">
              <mat-label>Cidade</mat-label>
              <input matInput formControlName="cidade" readonly>
            </mat-form-field>
            <mat-form-field appearance="outline" style="width:80px">
              <mat-label>UF</mat-label>
              <input matInput formControlName="uf" readonly>
            </mat-form-field>
          </div>

          @if (!editando) {
            <h3 style="margin-top:16px">Unidade Consumidora Inicial</h3>
            <div formGroupName="uc" class="row">
              <mat-form-field appearance="outline" class="flex-2">
                <mat-label>Nome da UC</mat-label>
                <input matInput formControlName="nome">
              </mat-form-field>
              <mat-form-field appearance="outline" class="flex-1">
                <mat-label>Nº Instalação</mat-label>
                <input matInput formControlName="numeroInstalacao">
              </mat-form-field>
            </div>
            <div formGroupName="uc">
              <div formGroupName="endereco" class="row">
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>CEP</mat-label>
                  <input matInput formControlName="cep" mask="00000-000" (blur)="buscarCepUC()">
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Número</mat-label>
                  <input matInput formControlName="numero">
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Complemento</mat-label>
                  <input matInput formControlName="complemento">
                </mat-form-field>
              </div>
              <div formGroupName="endereco" class="row readonly-fields">
                <mat-form-field appearance="outline" class="flex-2">
                  <mat-label>Logradouro</mat-label>
                  <input matInput formControlName="logradouro" readonly>
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Bairro</mat-label>
                  <input matInput formControlName="bairro" readonly>
                </mat-form-field>
                <mat-form-field appearance="outline" class="flex-1">
                  <mat-label>Cidade</mat-label>
                  <input matInput formControlName="cidade" readonly>
                </mat-form-field>
                <mat-form-field appearance="outline" style="width:80px">
                  <mat-label>UF</mat-label>
                  <input matInput formControlName="uf" readonly>
                </mat-form-field>
              </div>
            </div>
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
    .readonly-fields input { color: #666; }
    .form-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
    .hint { color: #888; font-size: 13px; margin-top: 8px; }
  `]
})
export class ClienteFormComponent implements OnInit {
  form!: FormGroup;
  editando = false;
  clienteId?: number;
  loading = signal(false);

  constructor(
    private fb: FormBuilder,
    private clienteService: ClienteService,
    private viaCepService: ViaCepService,
    private snackBar: MatSnackBar,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.form = this.fb.group({
      nome: ['', Validators.required],
      documento: ['', Validators.required],
      endereco: this.criarEnderecoGroup(),
      uc: this.fb.group({
        nome: ['', Validators.required],
        numeroInstalacao: ['', Validators.required],
        endereco: this.criarEnderecoGroup()
      })
    });

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.editando = true;
      this.clienteId = +id;
      this.form.removeControl('uc');
      this.clienteService.buscarPorId(this.clienteId).subscribe(c => {
        this.form.patchValue({ nome: c.nome, documento: c.documento, endereco: c.endereco });
      });
    }
  }

  criarEnderecoGroup() {
    return this.fb.group({
      cep: ['', Validators.required], numero: ['', Validators.required], complemento: [''],
      logradouro: [''], bairro: [''], cidade: [''], uf: ['']
    });
  }

  buscarCep() {
    const cep = this.form.get('endereco.cep')?.value;
    if (!cep) return;
    this.viaCepService.consultarCep(cep).subscribe(r => {
      if (r && !r.erro) this.form.get('endereco')?.patchValue({ logradouro: r.logradouro, bairro: r.bairro, cidade: r.localidade, uf: r.uf });
    });
  }

  buscarCepUC() {
    const cep = this.form.get('uc.endereco.cep')?.value;
    if (!cep) return;
    this.viaCepService.consultarCep(cep).subscribe(r => {
      if (r && !r.erro) this.form.get('uc.endereco')?.patchValue({ logradouro: r.logradouro, bairro: r.bairro, cidade: r.localidade, uf: r.uf });
    });
  }

  docMask(): string {
    const doc = this.form.get('documento')?.value?.replace(/\D/g, '') || '';
    return doc.length > 11 ? '00.000.000/0000-00' : '000.000.000-009';
  }

  onSubmit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.loading.set(true);
    const val = this.form.value;

    if (this.editando) {
      const request: ClienteUpdateRequest = {
        nome: val.nome, documento: val.documento,
        endereco: { cep: val.endereco.cep, numero: val.endereco.numero, complemento: val.endereco.complemento }
      };
      this.clienteService.atualizar(this.clienteId!, request).subscribe({
        next: () => { this.loading.set(false); this.snackBar.open('Cliente atualizado!', 'OK', { duration: 3000 }); this.router.navigate(['/clientes', this.clienteId]); },
        error: (err) => { this.loading.set(false); this.snackBar.open(err.error?.detail || 'Erro ao salvar', 'OK', { duration: 5000 }); }
      });
    } else {
      const request: ClienteRequest = {
        nome: val.nome, documento: val.documento,
        endereco: { cep: val.endereco.cep, numero: val.endereco.numero, complemento: val.endereco.complemento },
        unidadesConsumidoras: [{ nome: val.uc.nome, numeroInstalacao: val.uc.numeroInstalacao, endereco: { cep: val.uc.endereco.cep, numero: val.uc.endereco.numero, complemento: val.uc.endereco.complemento } }]
      };
      this.clienteService.criar(request).subscribe({
        next: (res: any) => {
          this.loading.set(false);
          if (res.cadastroPendenteId) { this.snackBar.open(`Cadastro em processamento (ID: ${res.cadastroPendenteId})`, 'OK', { duration: 5000 }); }
          else { this.snackBar.open('Cliente criado!', 'OK', { duration: 3000 }); }
          this.router.navigate(['/clientes']);
        },
        error: (err) => { this.loading.set(false); this.snackBar.open(err.error?.detail || err.error?.errors?.[0]?.message || 'Erro ao salvar', 'OK', { duration: 5000 }); }
      });
    }
  }

  voltar() { this.router.navigate(['/clientes']); }
}
