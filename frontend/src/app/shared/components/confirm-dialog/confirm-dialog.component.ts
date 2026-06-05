import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  icon?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warn' | 'info';
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog">
      <div class="confirm-icon" [ngClass]="data.variant || 'danger'">
        <mat-icon>{{ data.icon || 'warning' }}</mat-icon>
      </div>
      <h3>{{ data.title }}</h3>
      <p>{{ data.message }}</p>
      <div class="confirm-actions">
        <button mat-button (click)="dialogRef.close(false)">{{ data.cancelLabel || 'Cancelar' }}</button>
        <button mat-raised-button [ngClass]="'btn-' + (data.variant || 'danger')" (click)="dialogRef.close(true)">
          {{ data.confirmLabel || 'Confirmar' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .confirm-dialog {
      padding: 28px;
      text-align: center;
      max-width: 380px;
    }
    .confirm-icon {
      width: 48px;
      height: 48px;
      border-radius: var(--r-md);
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px;
      mat-icon { font-size: 24px; width: 24px; height: 24px; }
    }
    .confirm-icon.danger { background: var(--danger-bg); color: var(--danger-fg); }
    .confirm-icon.warn   { background: var(--warn-bg);   color: var(--warn-fg); }
    .confirm-icon.info   { background: var(--info-bg);   color: var(--info-fg); }

    h3 {
      font-family: var(--font-display);
      font-size: 17px;
      font-weight: 700;
      color: var(--text);
      margin: 0 0 8px;
    }
    p {
      font-size: 14px;
      color: var(--text-soft);
      margin: 0 0 24px;
      line-height: 1.5;
    }
    .confirm-actions {
      display: flex;
      justify-content: center;
      gap: 10px;
    }
    .btn-danger {
      background: var(--danger-fg) !important;
      color: white !important;
      border-radius: var(--r-pill) !important;
    }
    .btn-warn {
      background: var(--warn-fg) !important;
      color: white !important;
      border-radius: var(--r-pill) !important;
    }
  `]
})
export class ConfirmDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}
}
