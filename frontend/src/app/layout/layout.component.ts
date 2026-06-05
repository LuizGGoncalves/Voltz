import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule, NavigationEnd } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { filter } from 'rxjs';
import { AuthService } from '../core/services/auth.service';
import { ThemeService } from '../core/services/theme.service';
import { ViaCepService } from '../core/services/viacep.service';
import { ViaCepBadgeComponent } from '../shared/components/viacep-badge/viacep-badge.component';

interface NavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule, MatTooltipModule, ViaCepBadgeComponent],
  template: `
    <div class="shell">
      <!-- Sidebar (sempre escura) -->
      <aside class="sidebar">
        <div class="sidebar-brand">
          <div class="logo">
            <div class="logo-icon">
              <mat-icon>bolt</mat-icon>
            </div>
            <span class="logo-text">Bolt Energy</span>
          </div>
        </div>

        <nav class="sidebar-nav">
          @for (item of navItems; track item.path) {
            <a class="nav-item" [routerLink]="item.path" routerLinkActive="nav-active"
               [routerLinkActiveOptions]="{ exact: item.path === '/clientes' }">
              <div class="nav-indicator"></div>
              <mat-icon class="nav-icon">{{ item.icon }}</mat-icon>
              <span class="nav-label">{{ item.label }}</span>
            </a>
          }
        </nav>

        <div class="sidebar-footer">
          <app-viacep-badge />
        </div>
      </aside>

      <!-- Main -->
      <div class="main">
        <!-- Topbar -->
        <header class="topbar">
          <div class="topbar-left">
            <span class="breadcrumb">{{ breadcrumb }}</span>
            <h1 class="page-title">{{ pageTitle }}</h1>
          </div>
          <div class="topbar-right">
            <button class="theme-toggle" (click)="themeService.toggle()"
                    [matTooltip]="themeService.theme() === 'light' ? 'Modo escuro' : 'Modo claro'">
              <mat-icon>{{ themeService.theme() === 'light' ? 'dark_mode' : 'light_mode' }}</mat-icon>
            </button>
            <div class="user-info">
              <div class="user-avatar">{{ userInitial }}</div>
              <span class="user-name">{{ authService.username() }}</span>
            </div>
            <button class="theme-toggle" (click)="onLogout()" matTooltip="Sair">
              <mat-icon>logout</mat-icon>
            </button>
          </div>
        </header>

        <!-- Content -->
        <main class="content page-enter">
          <router-outlet />
        </main>
      </div>
    </div>
  `,
  styles: [`
    .shell {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }

    // ---- Sidebar ----
    .sidebar {
      width: 248px;
      min-width: 248px;
      background: var(--sidebar);
      display: flex;
      flex-direction: column;
      overflow-y: auto;
    }

    .sidebar-brand {
      padding: 22px 20px 18px;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .logo-icon {
      width: 36px;
      height: 36px;
      border-radius: 10px;
      background: var(--bolt-500);
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-onbrand);
      mat-icon { font-size: 20px; width: 20px; height: 20px; }
    }

    .logo-text {
      font-family: var(--font-display);
      font-size: 17px;
      font-weight: 700;
      color: var(--sidebar-fg-strong);
      letter-spacing: -0.02em;
    }

    .sidebar-nav {
      flex: 1;
      padding: 8px 10px;
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .nav-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 14px;
      border-radius: var(--r-sm);
      color: var(--sidebar-fg);
      text-decoration: none;
      font-size: 14px;
      font-weight: 500;
      transition: background var(--dur) var(--ease), color var(--dur) var(--ease);
      position: relative;

      &:hover {
        background: rgba(255, 255, 255, .06);
        color: var(--sidebar-fg-strong);
      }
    }

    .nav-indicator {
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      border-radius: 0 3px 3px 0;
      background: var(--accent-500);
      transition: height var(--dur) var(--ease);
    }

    .nav-active {
      background: var(--sidebar-active-bg);
      color: var(--sidebar-active-fg);

      .nav-indicator {
        height: 20px;
      }
      .nav-icon {
        color: var(--sidebar-active-fg);
      }
    }

    .nav-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
      color: var(--sidebar-fg);
      transition: color var(--dur) var(--ease);
    }

    .sidebar-footer {
      padding: 16px 20px;
      border-top: 1px solid rgba(255, 255, 255, .08);
      margin-top: auto;
    }

    // ---- Main area ----
    .main {
      flex: 1;
      display: flex;
      flex-direction: column;
      overflow-y: auto;
      background: var(--bg);
    }

    // ---- Topbar ----
    .topbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 66px;
      padding: 0 28px;
      background: var(--surface);
      border-bottom: 1px solid var(--border);
      flex-shrink: 0;
    }

    .topbar-left {
      display: flex;
      flex-direction: column;
      gap: 0;
    }

    .breadcrumb {
      font-size: 12px;
      font-weight: 500;
      color: var(--text-faint);
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .page-title {
      font-family: var(--font-display);
      font-size: 20px;
      font-weight: 700;
      color: var(--text);
      letter-spacing: -0.02em;
      line-height: 1.2;
    }

    .topbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .theme-toggle {
      width: 38px;
      height: 38px;
      border: none;
      border-radius: 10px;
      background: transparent;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-soft);
      transition: background var(--dur) var(--ease);

      &:hover {
        background: var(--surface-2);
      }
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .user-avatar {
      width: 34px;
      height: 34px;
      border-radius: 9px;
      background: var(--bolt-500);
      color: var(--text-onbrand);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
      font-weight: 700;
      font-family: var(--font-ui);
    }

    .user-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--text);
    }

    // ---- Content ----
    .content {
      padding: 26px 28px;
      max-width: 1240px;
      width: 100%;
    }
  `]
})
export class LayoutComponent implements OnInit, OnDestroy {
  private intervalId?: ReturnType<typeof setInterval>;

  navItems: NavItem[] = [
    { path: '/clientes', label: 'Clientes', icon: 'people' },
    { path: '/pendentes', label: 'Pendentes', icon: 'schedule' },
    { path: '/analises-mg', label: 'Análises MG', icon: 'assessment' },
  ];

  pageTitle = 'Clientes';
  breadcrumb = 'Gestão';
  userInitial = '';

  constructor(
    public authService: AuthService,
    public themeService: ThemeService,
    private viaCepService: ViaCepService,
    private router: Router
  ) {}

  ngOnInit() {
    this.viaCepService.carregarStatus();
    this.intervalId = setInterval(() => this.viaCepService.carregarStatus(), 30000);
    this.userInitial = (this.authService.username() || 'U').charAt(0).toUpperCase();
    this.updatePageInfo(this.router.url);
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(e => this.updatePageInfo(e.urlAfterRedirects));
  }

  ngOnDestroy() {
    if (this.intervalId) clearInterval(this.intervalId);
  }

  onLogout() {
    this.authService.logout().subscribe();
  }

  private updatePageInfo(url: string) {
    if (url.includes('/clientes/novo')) {
      this.pageTitle = 'Novo Cliente';
      this.breadcrumb = 'Clientes';
    } else if (url.match(/\/clientes\/\d+\/editar/)) {
      this.pageTitle = 'Editar Cliente';
      this.breadcrumb = 'Clientes';
    } else if (url.match(/\/clientes\/\d+$/)) {
      this.pageTitle = 'Detalhe';
      this.breadcrumb = 'Clientes';
    } else if (url.includes('/clientes')) {
      this.pageTitle = 'Clientes';
      this.breadcrumb = 'Gestão';
    } else if (url.includes('/pendentes')) {
      this.pageTitle = 'Pendentes';
      this.breadcrumb = 'Gestão';
    } else if (url.includes('/analises-mg')) {
      this.pageTitle = 'Análises MG';
      this.breadcrumb = 'Gestão';
    }
  }
}
