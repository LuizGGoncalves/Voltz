import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { LoginRequest, LoginResponse } from '../models/cliente.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private accessToken: string | null = null;
  readonly isLoggedIn = signal(false);
  readonly username = signal('');

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest) {
    return this.http.post<LoginResponse>('/api/v1/auth/login', request, { withCredentials: true }).pipe(
      tap(res => {
        this.accessToken = res.accessToken;
        this.isLoggedIn.set(true);
        this.parseUsername(res.accessToken);
      })
    );
  }

  refresh() {
    return this.http.post<LoginResponse>('/api/v1/auth/refresh', {}, { withCredentials: true }).pipe(
      tap(res => {
        this.accessToken = res.accessToken;
        this.isLoggedIn.set(true);
        this.parseUsername(res.accessToken);
      })
    );
  }

  logout() {
    return this.http.post<void>('/api/v1/auth/logout', {}, { withCredentials: true }).pipe(
      tap(() => {
        this.accessToken = null;
        this.isLoggedIn.set(false);
        this.username.set('');
        this.router.navigate(['/login']);
      })
    );
  }

  getToken(): string | null {
    return this.accessToken;
  }

  private parseUsername(token: string) {
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.username.set(payload.sub || '');
    } catch {
      this.username.set('');
    }
  }
}
