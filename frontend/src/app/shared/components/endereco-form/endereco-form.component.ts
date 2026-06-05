import { Component, Input, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgxMaskDirective } from 'ngx-mask';
import { ViaCepService } from '../../../core/services/viacep.service';

@Component({
  selector: 'app-endereco-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatProgressSpinnerModule, NgxMaskDirective],
  template: `
    <div [formGroup]="form" class="row">
      <mat-form-field appearance="outline" class="flex-1">
        <mat-label>CEP</mat-label>
        <input matInput formControlName="cep" mask="00000-000" (blur)="buscarCep()">
        @if (buscando()) {
          <mat-spinner matSuffix diameter="18"></mat-spinner>
        }
        <mat-hint>Digite o CEP e clique fora para buscar</mat-hint>
      </mat-form-field>
      <mat-form-field appearance="outline" class="flex-1">
        <mat-label>Numero</mat-label>
        <input matInput formControlName="numero">
      </mat-form-field>
      <mat-form-field appearance="outline" class="flex-1">
        <mat-label>Complemento</mat-label>
        <input matInput formControlName="complemento">
      </mat-form-field>
    </div>

    @if (feedback()) {
      <div class="feedback" [class.sucesso]="feedbackTipo() === 'sucesso'" [class.erro]="feedbackTipo() === 'erro'">
        <mat-icon>{{ feedbackTipo() === 'sucesso' ? 'check_circle' : 'error' }}</mat-icon>
        <span>{{ feedback() }}</span>
      </div>
    }

    <div [formGroup]="form" class="row campos-auto" [class.preenchido]="form.get('logradouro')?.value">
      <mat-form-field appearance="outline" class="flex-2 field-readonly">
        <mat-label>Logradouro</mat-label>
        <input matInput formControlName="logradouro" readonly tabindex="-1">
      </mat-form-field>
      <mat-form-field appearance="outline" class="flex-1 field-readonly">
        <mat-label>Bairro</mat-label>
        <input matInput formControlName="bairro" readonly tabindex="-1">
      </mat-form-field>
      <mat-form-field appearance="outline" class="flex-1 field-readonly">
        <mat-label>Cidade</mat-label>
        <input matInput formControlName="cidade" readonly tabindex="-1">
      </mat-form-field>
      <mat-form-field appearance="outline" class="field-readonly" style="width:80px">
        <mat-label>UF</mat-label>
        <input matInput formControlName="uf" readonly tabindex="-1">
      </mat-form-field>
    </div>
  `,
  styles: [`
    .campos-auto {
      opacity: 0.5;
      transition: opacity 0.3s var(--ease);
    }
    .campos-auto.preenchido {
      opacity: 1;
    }

    .feedback {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      margin: 4px 0 14px;
      border-radius: var(--r-md);
      font-size: 13px;
      font-weight: 500;
      font-family: var(--font-ui);
      animation: fadeIn 0.26s var(--ease);
    }
    .feedback mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .feedback.sucesso { background: var(--ok-bg); color: var(--ok-fg); }
    .feedback.erro { background: var(--danger-bg); color: var(--danger-fg); }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class EnderecoFormComponent {
  @Input({ required: true }) form!: FormGroup;

  buscando = signal(false);
  feedback = signal('');
  feedbackTipo = signal<'sucesso' | 'erro'>('sucesso');

  constructor(private viaCepService: ViaCepService) {}

  buscarCep() {
    const cep = this.form.get('cep')?.value?.replace(/\D/g, '');
    if (!cep || cep.length < 8) return;

    this.buscando.set(true);
    this.feedback.set('');

    this.viaCepService.consultarCep(cep).subscribe({
      next: r => {
        this.buscando.set(false);
        if (r && !r.erro) {
          this.form.patchValue({
            logradouro: r.logradouro, bairro: r.bairro,
            cidade: r.localidade, uf: r.uf
          });
          this.feedbackTipo.set('sucesso');
          this.feedback.set(`Endereço encontrado: ${r.logradouro}, ${r.localidade}/${r.uf}`);
        } else {
          this.limparCamposAuto();
          this.feedbackTipo.set('erro');
          this.feedback.set('CEP não encontrado. Verifique e tente novamente.');
        }
      },
      error: () => {
        this.buscando.set(false);
        this.limparCamposAuto();
        this.feedbackTipo.set('erro');
        this.feedback.set('Não foi possível consultar o CEP. Tente novamente.');
      }
    });
  }

  private limparCamposAuto() {
    this.form.patchValue({ logradouro: '', bairro: '', cidade: '', uf: '' });
  }

  static createGroup(fb: FormBuilder): FormGroup {
    return fb.group({
      cep: ['', Validators.required],
      numero: ['', Validators.required],
      complemento: [''],
      logradouro: [''],
      bairro: [''],
      cidade: [''],
      uf: ['']
    });
  }
}
