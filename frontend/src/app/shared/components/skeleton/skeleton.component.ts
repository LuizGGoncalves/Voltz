import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-table">
      @for (row of rows; track row) {
        <div class="skeleton-row">
          @for (col of cols; track col) {
            <div class="skeleton-cell" [style.flex]="col"></div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .skeleton-table { display: flex; flex-direction: column; gap: 2px; }
    .skeleton-row {
      display: flex;
      gap: 16px;
      padding: 16px;
      border-bottom: 1px solid var(--border);
    }
    .skeleton-cell {
      height: 16px;
      border-radius: var(--r-sm);
      background: linear-gradient(90deg, var(--surface-2) 25%, var(--border) 50%, var(--surface-2) 75%);
      background-size: 200% 100%;
      animation: shimmer 1.4s infinite;
    }

    @keyframes shimmer {
      0% { background-position: 200% 0; }
      100% { background-position: -200% 0; }
    }
  `]
})
export class SkeletonComponent {
  @Input() rowCount = 5;
  @Input() columns: number[] = [3, 2, 1, 2, 1];

  get rows() { return Array(this.rowCount); }
  get cols() { return this.columns; }
}
