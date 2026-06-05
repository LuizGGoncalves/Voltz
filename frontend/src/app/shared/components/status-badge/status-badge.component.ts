import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="semanticClass">
      <span class="dot"></span>
      {{ displayLabel }}
    </span>
  `,
  styles: [`
    .badge {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 4px 12px;
      border-radius: var(--r-pill);
      font-family: var(--font-ui);
      font-size: 12px;
      font-weight: 700;
      letter-spacing: 0.01em;
      white-space: nowrap;
    }
    .dot {
      width: 7px;
      height: 7px;
      border-radius: 50%;
      flex-shrink: 0;
    }
    .ok   { background: var(--ok-bg);     color: var(--ok-fg); }
    .ok   .dot { background: var(--ok-fg); }
    .warn { background: var(--warn-bg);   color: var(--warn-fg); }
    .warn .dot { background: var(--warn-fg); }
    .danger { background: var(--danger-bg); color: var(--danger-fg); }
    .danger .dot { background: var(--danger-fg); }
    .crit { background: var(--crit-bg);   color: var(--crit-fg); }
    .crit .dot { background: var(--crit-fg); }
    .info { background: var(--info-bg);   color: var(--info-fg); }
    .info .dot { background: var(--info-fg); }
  `]
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: string;
  @Input() label?: string;

  private static readonly LABEL_MAP: Record<string, string> = {
    ativo: 'Ativo', inativo: 'Inativo', pendente: 'Pendente',
    processado: 'Processado', rejeitado: 'Rejeitado', falha: 'Falha',
    pendente_analise: 'Pendente análise',
  };

  private static readonly SEMANTIC_MAP: Record<string, string> = {
    ativo: 'ok', processado: 'ok',
    pendente: 'warn', pendente_analise: 'warn',
    inativo: 'danger', rejeitado: 'danger',
    falha: 'crit',
  };

  get displayLabel(): string {
    return this.label || StatusBadgeComponent.LABEL_MAP[this.normalizedStatus] || this.status;
  }

  get semanticClass(): string {
    return StatusBadgeComponent.SEMANTIC_MAP[this.normalizedStatus] || 'info';
  }

  private get normalizedStatus(): string {
    return this.status.toLowerCase().replace(' ', '_');
  }
}
