import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="empty-state">
      <mat-icon>{{ icon }}</mat-icon>
      <p>{{ message }}</p>
    </div>
  `,
  styles: [`
    .empty-state {
      display: flex; flex-direction: column; align-items: center;
      padding: 48px 16px; color: #999;
    }
    mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; }
    p { font-size: 14px; margin: 0; }
  `]
})
export class EmptyStateComponent {
  @Input() icon = 'inbox';
  @Input() message = 'Nenhum registro encontrado.';
}
