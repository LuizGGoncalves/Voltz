import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { ViaCepEndereco, ViaCepStatus } from '../models/cliente.model';

@Injectable({ providedIn: 'root' })
export class ViaCepService {
  readonly status = signal<ViaCepStatus>({ disponivel: true, ultimaVerificacao: '' });

  constructor(private http: HttpClient) {}

  consultarCep(cep: string): Observable<ViaCepEndereco | null> {
    const cepLimpo = cep.replace(/\D/g, '');
    if (cepLimpo.length !== 8) return of(null);
    return this.http.get<ViaCepEndereco>(`https://viacep.com.br/ws/${cepLimpo}/json`).pipe(
      catchError(() => of(null))
    );
  }

  carregarStatus() {
    this.http.get<ViaCepStatus>('/api/v1/integracoes/viacep/status').pipe(
      catchError(() => of({ disponivel: false, ultimaVerificacao: new Date().toISOString() }))
    ).subscribe(s => this.status.set(s));
  }
}
