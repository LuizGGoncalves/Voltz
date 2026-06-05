import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span class="badge" [ngClass]="cssClass">{{ label }}</span>`,
  styles: [`
    .badge { padding: 4px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; white-space: nowrap; }
    .ativo { background: #e8f5e9; color: #2e7d32; }
    .inativo { background: #fce4ec; color: #c62828; }
    .pendente { background: #fff3e0; color: #e65100; }
    .processado { background: #e8f5e9; color: #2e7d32; }
    .rejeitado { background: #fce4ec; color: #c62828; }
    .falha { background: #ffebee; color: #b71c1c; }
    .pendente_analise { background: #fff3e0; color: #e65100; }
  `]
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;
  @Input() label?: string;

  get cssClass(): string {
    return this.status.toLowerCase().replace(' ', '_');
  }
}
