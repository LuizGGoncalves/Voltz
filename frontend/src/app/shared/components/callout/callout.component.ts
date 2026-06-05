import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-callout',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="callout" [ngClass]="variant">
      <div class="callout-icon" [ngClass]="variant">
        <mat-icon>{{ icon }}</mat-icon>
      </div>
      <div class="callout-content">
        <ng-content></ng-content>
      </div>
      <div class="callout-right">
        <ng-content select="[slot=right]"></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .callout {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 14px 18px;
      border-radius: 14px;
      margin-bottom: 20px;
      font-size: 13.5px;
      font-family: var(--font-ui);
    }
    .callout.brand { background: var(--bolt-tint); color: var(--text-soft); }
    .callout.accent { background: var(--accent-tint); color: var(--text-soft); }
    .callout.info { background: var(--info-bg); color: var(--text-soft); }

    .callout-icon {
      width: 32px;
      height: 32px;
      border-radius: 9px;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
      mat-icon { font-size: 18px; width: 18px; height: 18px; color: var(--text-onbrand); }
    }
    .callout-icon.brand { background: var(--bolt-500); }
    .callout-icon.accent { background: var(--accent-600); }
    .callout-icon.info { background: var(--info-fg); }

    .callout-content { flex: 1; line-height: 1.5; }
    .callout-right { flex-shrink: 0; }
  `]
})
export class CalloutComponent {
  @Input() icon = 'info';
  @Input() variant: 'brand' | 'accent' | 'info' = 'brand';
}
