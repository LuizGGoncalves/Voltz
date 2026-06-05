import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    loadComponent: () => import('./layout/layout.component').then(m => m.LayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: 'clientes', loadComponent: () => import('./features/clientes/lista/cliente-lista.component').then(m => m.ClienteListaComponent) },
      { path: 'clientes/novo', loadComponent: () => import('./features/clientes/form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'clientes/:id', loadComponent: () => import('./features/clientes/detalhe/cliente-detalhe.component').then(m => m.ClienteDetalheComponent) },
      { path: 'clientes/:id/editar', loadComponent: () => import('./features/clientes/form/cliente-form.component').then(m => m.ClienteFormComponent) },
      { path: 'pendentes', loadComponent: () => import('./features/pendentes/pendentes.component').then(m => m.PendentesComponent) },
      { path: 'analises-mg', loadComponent: () => import('./features/analise-mg/analise-mg.component').then(m => m.AnaliseMgComponent) },
      { path: '', redirectTo: 'clientes', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: 'login' }
];
