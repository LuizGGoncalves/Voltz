import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <div class="login-shell">
      <!-- Painel de marca -->
      <div class="brand-panel">
        <div class="brand-content">
          <div class="brand-logo">
            <div class="logo-icon">
              <mat-icon>bolt</mat-icon>
            </div>
            <span class="logo-text">Bolt Energy</span>
          </div>
          <h1 class="brand-headline">Gestão inteligente de clientes e unidades consumidoras</h1>
          <p class="brand-sub">Cadastre, acompanhe e gerencie toda sua base de clientes com segurança e eficiência.</p>
        </div>
        <div class="glow"></div>
      </div>

      <!-- Formulário -->
      <div class="form-panel">
        <div class="form-container">
          <h2>Entrar</h2>
          <p class="form-subtitle">Informe suas credenciais para acessar o sistema</p>

          @if (erro()) {
            <div class="error-banner">
              <mat-icon>error</mat-icon>
              <span>{{ erro() }}</span>
            </div>
          }

          <form (ngSubmit)="onSubmit()" class="login-form">
            <div class="field-group">
              <label for="username">Usuário <span class="required">*</span></label>
              <mat-form-field appearance="outline" class="full-width">
                <input matInput id="username" [(ngModel)]="username" name="username" required autofocus
                       placeholder="Digite seu usuário">
              </mat-form-field>
            </div>

            <div class="field-group">
              <label for="password">Senha <span class="required">*</span></label>
              <mat-form-field appearance="outline" class="full-width">
                <input matInput id="password" [type]="showPassword ? 'text' : 'password'"
                       [(ngModel)]="password" name="password" required
                       placeholder="Digite sua senha">
                <button type="button" mat-icon-button matSuffix (click)="showPassword = !showPassword" tabindex="-1">
                  <mat-icon>{{ showPassword ? 'visibility_off' : 'visibility' }}</mat-icon>
                </button>
              </mat-form-field>
            </div>

            <button mat-raised-button color="primary" type="submit" class="full-width submit-btn" [disabled]="loading()">
              @if (loading()) {
                <mat-spinner diameter="20"></mat-spinner>
              } @else {
                Entrar
              }
            </button>
          </form>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-shell {
      display: flex;
      height: 100vh;
      overflow: hidden;
    }

    // -- Painel de marca --
    .brand-panel {
      flex: 1;
      background: var(--sidebar);
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
      overflow: hidden;
      padding: 40px;
    }

    .brand-content {
      position: relative;
      z-index: 1;
      max-width: 420px;
    }

    .brand-logo {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 40px;
    }

    .logo-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: var(--bolt-500);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      mat-icon { font-size: 24px; width: 24px; height: 24px; }
    }

    .logo-text {
      font-family: var(--font-display);
      font-size: 22px;
      font-weight: 700;
      color: white;
      letter-spacing: -0.02em;
    }

    .brand-headline {
      font-family: var(--font-display);
      font-size: 32px;
      font-weight: 700;
      color: white;
      line-height: 1.2;
      letter-spacing: -0.03em;
      margin: 0 0 16px;
    }

    .brand-sub {
      font-size: 16px;
      color: var(--sidebar-fg);
      line-height: 1.6;
      margin: 0;
    }

    .glow {
      position: absolute;
      bottom: -120px;
      left: -60px;
      width: 400px;
      height: 400px;
      background: radial-gradient(circle, rgba(48, 239, 188, .15) 0%, transparent 70%);
      pointer-events: none;
    }

    // -- Painel do form --
    .form-panel {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg);
      padding: 40px;
    }

    .form-container {
      width: 100%;
      max-width: 400px;
    }

    h2 {
      font-family: var(--font-display);
      font-size: 24px;
      font-weight: 700;
      color: var(--text);
      margin: 0 0 6px;
    }

    .form-subtitle {
      font-size: 14px;
      color: var(--text-soft);
      margin: 0 0 28px;
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 14px;
      background: var(--danger-bg);
      color: var(--danger-fg);
      border-radius: var(--r-md);
      font-size: 13px;
      font-weight: 500;
      margin-bottom: 20px;
      animation: fadeIn 0.26s var(--ease);

      mat-icon { font-size: 18px; width: 18px; height: 18px; }
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .field-group {
      display: flex;
      flex-direction: column;
      gap: 4px;

      label {
        font-size: 13px;
        font-weight: 600;
        color: var(--text-soft);
      }
      .required { color: var(--danger-fg); }
    }

    .full-width { width: 100%; }

    .submit-btn {
      margin-top: 8px;
      height: 46px !important;
      font-size: 15px !important;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(-4px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @media (max-width: 768px) {
      .login-shell { flex-direction: column; }
      .brand-panel { display: none; }
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  showPassword = false;
  loading = signal(false);
  erro = signal('');

  constructor(private authService: AuthService, private router: Router) {}

  onSubmit() {
    if (!this.username || !this.password) return;
    this.loading.set(true);
    this.erro.set('');
    this.authService.login({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate(['/clientes']);
      },
      error: () => {
        this.loading.set(false);
        this.erro.set('Usuário ou senha inválidos');
      }
    });
  }
}
