import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { AuthService } from '../core/services/auth.service';
import { ViaCepService } from '../core/services/viacep.service';
import { ViaCepBadgeComponent } from '../shared/components/viacep-badge/viacep-badge.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule, MatSidenavModule, MatListModule, ViaCepBadgeComponent],
  template: `
    <mat-sidenav-container class="layout-container">
      <mat-sidenav mode="side" opened class="sidenav">
        <div class="sidenav-header">
          <h3>Gestão de Clientes</h3>
        </div>
        <mat-nav-list>
          <a mat-list-item routerLink="/clientes" routerLinkActive="active">
            <mat-icon matListItemIcon>people</mat-icon>
            <span matListItemTitle>Clientes</span>
          </a>
          <a mat-list-item routerLink="/pendentes" routerLinkActive="active">
            <mat-icon matListItemIcon>hourglass_empty</mat-icon>
            <span matListItemTitle>Pendentes</span>
          </a>
          <a mat-list-item routerLink="/analises-mg" routerLinkActive="active">
            <mat-icon matListItemIcon>assignment</mat-icon>
            <span matListItemTitle>Análises MG</span>
          </a>
        </mat-nav-list>
        <div class="sidenav-footer">
          <app-viacep-badge />
        </div>
      </mat-sidenav>
      <mat-sidenav-content>
        <mat-toolbar color="primary">
          <span class="toolbar-spacer"></span>
          <span class="username">{{ authService.username() }}</span>
          <button mat-icon-button (click)="onLogout()"><mat-icon>logout</mat-icon></button>
        </mat-toolbar>
        <main class="content">
          <router-outlet />
        </main>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .layout-container { height: 100vh; }
    .sidenav { width: 240px; }
    .sidenav-header { padding: 16px; border-bottom: 1px solid #e0e0e0; }
    .sidenav-header h3 { margin: 0; font-size: 16px; }
    .sidenav-footer { padding: 16px; margin-top: auto; border-top: 1px solid #e0e0e0; }
    .toolbar-spacer { flex: 1; }
    .username { margin-right: 8px; font-size: 14px; }
    .content { padding: 24px; }
    .active { background: rgba(0,0,0,0.04); }
  `]
})
export class LayoutComponent implements OnInit, OnDestroy {
  private intervalId?: ReturnType<typeof setInterval>;

  constructor(public authService: AuthService, private viaCepService: ViaCepService) {}

  ngOnInit() {
    this.viaCepService.carregarStatus();
    this.intervalId = setInterval(() => this.viaCepService.carregarStatus(), 30000);
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  onLogout() {
    this.authService.logout().subscribe();
  }
}
