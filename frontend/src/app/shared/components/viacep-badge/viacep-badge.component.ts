import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ViaCepService } from '../../../core/services/viacep.service';

@Component({
  selector: 'app-viacep-badge',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <div class="viacep-status" [matTooltip]="tooltip()">
      <span class="status-dot" [class.online]="isOnline()" [class.offline]="!isOnline()"></span>
      <span class="status-label">ViaCEP: {{ isOnline() ? 'Disponível' : 'Indisponível' }}</span>
    </div>
  `,
  styles: [`
    .viacep-status {
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: help;
    }
    .status-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .status-dot.online { background: var(--accent-500); }
    .status-dot.offline { background: var(--warn-fg); }
    .status-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--sidebar-fg);
    }
  `]
})
export class ViaCepBadgeComponent {
  constructor(private viaCepService: ViaCepService) {}

  isOnline() { return this.viaCepService.status().disponivel; }
  tooltip() {
    return this.isOnline()
      ? 'Consulta de CEP funcionando normalmente.'
      : 'Consulta de CEP indisponível. Cadastros serão processados automaticamente quando o serviço voltar.';
  }
}
