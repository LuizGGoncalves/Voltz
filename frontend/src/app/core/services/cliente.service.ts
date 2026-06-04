import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Cliente, ClienteResumo, ClienteRequest, Page,
  CadastroPendente, CadastroPendenteCreated, AnaliseMg
} from '../models/cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly baseUrl = '/api/v1/clientes';

  constructor(private http: HttpClient) {}

  criar(request: ClienteRequest): Observable<Cliente | CadastroPendenteCreated> {
    return this.http.post<Cliente | CadastroPendenteCreated>(this.baseUrl, request, { observe: 'body' });
  }

  atualizar(id: number, request: ClienteRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.baseUrl}/${id}`, request);
  }

  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.baseUrl}/${id}`);
  }

  listar(page = 0, size = 20, incluirInativos = false): Observable<Page<ClienteResumo>> {
    const params = new HttpParams()
      .set('page', page).set('size', size)
      .set('incluirInativos', incluirInativos);
    return this.http.get<Page<ClienteResumo>>(this.baseUrl, { params });
  }

  ultimos20(): Observable<Page<ClienteResumo>> {
    return this.http.get<Page<ClienteResumo>>(`${this.baseUrl}/ultimos`);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  listarPendentes(page = 0, size = 20, status?: string): Observable<Page<CadastroPendente>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<Page<CadastroPendente>>('/api/v1/cadastros-pendentes', { params });
  }

  listarAnalisesMg(page = 0, size = 20): Observable<Page<AnaliseMg>> {
    return this.http.get<Page<AnaliseMg>>('/api/v1/analises-mg', {
      params: new HttpParams().set('page', page).set('size', size)
    });
  }
}
