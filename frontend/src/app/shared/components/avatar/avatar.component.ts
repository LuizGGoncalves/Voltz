import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    <div class="avatar" [style.background]="bgColor" [style.width.px]="size" [style.height.px]="size"
         [style.font-size.px]="size * 0.4" [style.border-radius.px]="size * 0.28">
      {{ initials }}
    </div>
  `,
  styles: [`
    .avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-onbrand);
      font-family: var(--font-ui);
      font-weight: 700;
      flex-shrink: 0;
    }
  `]
})
export class AvatarComponent {
  @Input({ required: true }) name!: string;
  @Input() size = 38;

  private static readonly PALETTE = [
    '#2B40F5', '#1d63c4', '#b8650b', '#8b5cf6', '#0a9c76', '#d946a0'
  ];

  get initials(): string {
    const parts = (this.name || '').trim().split(/\s+/);
    if (parts.length >= 2) return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
    return (parts[0]?.[0] || '?').toUpperCase();
  }

  get bgColor(): string {
    let hash = 0;
    for (const ch of this.name || '') hash = ch.charCodeAt(0) + ((hash << 5) - hash);
    return AvatarComponent.PALETTE[Math.abs(hash) % AvatarComponent.PALETTE.length];
  }
}
