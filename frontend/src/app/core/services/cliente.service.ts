import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  Cliente, ClienteResumo, ClienteRequest, Page,
  CadastroPendente, CadastroPendenteCreated, AnaliseMg,
  UnidadeConsumidora, UnidadeConsumidoraRequest, ClienteUpdateRequest
} from '../models/cliente.model';

@Injectable({ providedIn: 'root' })
export class ClienteService {
  private readonly baseUrl = '/api/v1/clientes';

  constructor(private http: HttpClient) {}

  criar(request: ClienteRequest): Observable<Cliente | CadastroPendenteCreated> {
    return this.http.post<Cliente | CadastroPendenteCreated>(this.baseUrl, request, { observe: 'body' });
  }

  atualizar(id: number, request: ClienteUpdateRequest): Observable<Cliente> {
    return this.http.put<Cliente>(`${this.baseUrl}/${id}`, request);
  }

  buscarPorId(id: number): Observable<Cliente> {
    return this.http.get<Cliente>(`${this.baseUrl}/${id}`);
  }

  listar(page = 0, size = 20, filtroStatus = 'ativos'): Observable<Page<ClienteResumo>> {
    const params = new HttpParams()
      .set('page', page).set('size', size)
      .set('filtroStatus', filtroStatus);
    return this.http.get<Page<ClienteResumo>>(this.baseUrl, { params });
  }

  ultimos20(): Observable<Page<ClienteResumo>> {
    return this.http.get<Page<ClienteResumo>>(`${this.baseUrl}/ultimos`);
  }

  inativar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // === Unidades Consumidoras ===

  listarUCs(clienteId: number): Observable<UnidadeConsumidora[]> {
    return this.http.get<UnidadeConsumidora[]>(`${this.baseUrl}/${clienteId}/unidades`);
  }

  adicionarUC(clienteId: number, request: UnidadeConsumidoraRequest): Observable<UnidadeConsumidora> {
    return this.http.post<UnidadeConsumidora>(`${this.baseUrl}/${clienteId}/unidades`, request);
  }

  atualizarUC(clienteId: number, ucId: number, request: UnidadeConsumidoraRequest): Observable<UnidadeConsumidora> {
    return this.http.put<UnidadeConsumidora>(`${this.baseUrl}/${clienteId}/unidades/${ucId}`, request);
  }

  removerUC(clienteId: number, ucId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${clienteId}/unidades/${ucId}`);
  }

  // === Pendentes e Análises ===

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
