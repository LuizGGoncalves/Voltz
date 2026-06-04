import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ViaCepService } from '../../../core/services/viacep.service';

@Component({
  selector: 'app-viacep-badge',
  standalone: true,
  imports: [CommonModule, MatChipsModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="viacep-status" [matTooltip]="tooltip()">
      <mat-icon [class]="statusClass()">{{ statusIcon() }}</mat-icon>
      <span class="label">ViaCEP: {{ statusText() }}</span>
    </div>
  `,
  styles: [`
    .viacep-status { display: flex; align-items: center; gap: 8px; font-size: 13px; cursor: help; }
    .online { color: #4caf50; }
    .offline { color: #ff9800; }
    .label { color: #666; }
  `]
})
export class ViaCepBadgeComponent {
  constructor(private viaCepService: ViaCepService) {}

  statusIcon() { return this.viaCepService.status().disponivel ? 'check_circle' : 'warning'; }
  statusClass() { return this.viaCepService.status().disponivel ? 'online' : 'offline'; }
  statusText() { return this.viaCepService.status().disponivel ? 'disponível' : 'indisponível'; }
  tooltip() {
    return this.viaCepService.status().disponivel
      ? 'Consulta de CEP funcionando normalmente.'
      : 'Consulta de CEP indisponível. Cadastros serão processados automaticamente quando o serviço voltar.';
  }
}
