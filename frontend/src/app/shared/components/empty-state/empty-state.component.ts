import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule, MatButtonModule],
  template: `
    <div class="empty-state">
      <div class="empty-icon-box">
        <mat-icon>{{ icon }}</mat-icon>
      </div>
      <p class="empty-title">{{ title || message }}</p>
      @if (subtitle) {
        <p class="empty-subtitle">{{ subtitle }}</p>
      }
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 56px 16px;
    }
    .empty-icon-box {
      width: 64px;
      height: 64px;
      border-radius: var(--r-lg);
      background: var(--surface-2);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 16px;

      mat-icon {
        font-size: 28px;
        width: 28px;
        height: 28px;
        color: var(--text-faint);
      }
    }
    .empty-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-soft);
      margin: 0 0 4px;
      text-align: center;
    }
    .empty-subtitle {
      font-size: 13px;
      color: var(--text-faint);
      margin: 0;
      text-align: center;
    }
  `]
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() message = 'Nenhum registro encontrado.';
  @Input() title?: string;
  @Input() subtitle?: string;
}
