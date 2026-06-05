export interface Endereco {
  cep: string;
  logradouro: string;
  numero: string;
  complemento?: string;
  bairro: string;
  cidade: string;
  uf: string;
}

export interface EnderecoRequest {
  cep: string;
  numero: string;
  complemento?: string;
}

export interface UnidadeConsumidora {
  id: number;
  nome: string;
  numeroInstalacao: string;
  endereco: Endereco;
  ativo: boolean;
}

export interface UnidadeConsumidoraRequest {
  nome: string;
  numeroInstalacao: string;
  endereco: EnderecoRequest;
}

export interface Cliente {
  id: number;
  nome: string;
  documento: string;
  tipoDocumento: string;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
  endereco: Endereco;
  unidadesConsumidoras: UnidadeConsumidora[];
}

export interface ClienteResumo {
  id: number;
  nome: string;
  documento: string;
  tipoDocumento: string;
  ativo: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ClienteRequest {
  nome: string;
  documento: string;
  endereco: EnderecoRequest;
  unidadesConsumidoras: UnidadeConsumidoraRequest[];
}

export interface ClienteUpdateRequest {
  nome: string;
  documento: string;
  endereco: EnderecoRequest;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CadastroPendente {
  id: number;
  documento: string;
  status: string;
  motivo?: string;
  tentativas: number;
  createdAt: string;
  ultimaTentativa?: string;
}

export interface CadastroPendenteCreated {
  cadastroPendenteId: number;
  status: string;
  mensagem: string;
}

export interface AnaliseMg {
  id: number;
  clienteId: number;
  unidadeConsumidoraId: number;
  status: string;
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface ViaCepStatus {
  disponivel: boolean;
  ultimaVerificacao: string;
}

export interface ViaCepEndereco {
  cep: string;
  logradouro: string;
  complemento: string;
  bairro: string;
  localidade: string;
  uf: string;
  erro?: boolean;
}
