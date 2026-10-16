import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { duenoGuard } from './guards/dueno.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./componentes/landing/landing.component').then(m => m.LandingComponent), pathMatch: 'full' },
  { path: 'login', title:'Ingresar | Somos Fútbol', loadComponent: () => import('./componentes/login/login.component').then(m => m.LoginComponent) },
  { path: 'register', title:'Crear cuenta | Somos Fútbol', loadComponent: () => import('./componentes/register/register.component').then(m => m.RegisterComponent) },
  {
    path: 'compartir',
    title: 'Compartir video | Somos Fútbol',
    loadComponent: () =>
      import('./componentes/compartir/compartir.component')
        .then(m => m.CompartirComponent)
  },
  {
    path: 'terminos-y-condiciones',
    loadComponent: () => import('./componentes/legal/terminos.component').then((m) => m.TerminosComponent),
  },
  {
    path: 'politica-de-privacidad',
    loadComponent: () => import('./componentes/legal/privacidad.component').then((m) => m.PrivacidadComponent),
  },
  {
    path: '',
    loadComponent: () => import('./layouts/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    canActivateChild: [authGuard],
    children: [
      { path: 'inicio', title:'Comunidad | Somos Fútbol', loadComponent: () => import('./componentes/feed/feed.component').then(m => m.FeedComponent) },
      { path: 'chat', title:'Mensajes | Somos Fútbol', loadComponent: () => import('./componentes/chat/chat.component').then(m => m.ChatComponent) },
      { path: 'canchas', title:'Buscar canchas | Somos Fútbol', loadComponent: () => import('./componentes/canchas/canchas.component').then(m => m.CanchasComponent) },
      { path: 'duenos/:id', loadComponent: () => import('./componentes/dueno-publico/dueno-publico.component').then(m => m.DuenoPublicoComponent), title:'Canchas del dueño | Somos Fútbol' },
      { path: 'canchas/:id', loadComponent: () => import('./componentes/detalle-cancha/detalle-cancha.component').then(m => m.DetalleCanchaComponent) },
      { path: 'reservas', title:'Mis reservas | Somos Fútbol', loadComponent: () => import('./componentes/reservas/reservas.component').then(m => m.ReservasComponent) },
      { path: 'perfil', title:'Mi perfil | Somos Fútbol', loadComponent: () => import('./componentes/perfil/perfil.component').then(m => m.PerfilComponent) },
      { path: 'amigos', title:'Amigos | Somos Fútbol', loadComponent: () => import('./componentes/amigos/amigos.component').then(m => m.AmigosComponent) },
      { path: 'notificaciones', title:'Notificaciones | Somos Fútbol', loadComponent: () => import('./componentes/notificaciones/notificaciones.component').then(m => m.NotificacionesComponent) },
      { path: 'mis-canchas', title:'Panel de dueño | Somos Fútbol', loadComponent: () => import('./componentes/dashboard-dueno/dashboard-dueno.component').then(m => m.DashboardDuenoComponent), canActivate: [duenoGuard] },
    ],
  },
  { path: '**', redirectTo: '' },
];
