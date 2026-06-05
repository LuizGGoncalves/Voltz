import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormGroup, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { NgxMaskDirective } from 'ngx-mask';
import { ViaCepService } from '../../../core/services/viacep.service';

@Component({
  selector: 'app-endereco-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatFormFieldModule, MatInputModule, NgxMaskDirective],
  template: `
    <div [formGroup]="form" class="row">
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
    <div [formGroup]="form" class="row">
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
  `,
  styles: [`
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .flex-1 { flex: 1; min-width: 130px; }
    .flex-2 { flex: 2; min-width: 200px; }
    input[readonly] { color: #666; }
  `]
})
export class EnderecoFormComponent {
  @Input({ required: true }) form!: FormGroup;

  constructor(private viaCepService: ViaCepService) {}

  buscarCep() {
    const cep = this.form.get('cep')?.value;
    if (!cep) return;
    this.viaCepService.consultarCep(cep).subscribe(r => {
      if (r && !r.erro) {
        this.form.patchValue({
          logradouro: r.logradouro, bairro: r.bairro,
          cidade: r.localidade, uf: r.uf
        });
      }
    });
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
