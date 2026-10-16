import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';

import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { EMPTY, Subscription, catchError, switchMap, timer } from 'rxjs';
import { AuthService, Usuario } from '../../services/auth.service';
import { NotificacionService } from '../../services/notificacion.service';

@Component({
    selector: 'app-main-layout',
    imports: [RouterLink, RouterLinkActive, RouterOutlet],
    templateUrl: './main-layout.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './main-layout.component.css'
})
export class MainLayoutComponent implements OnInit, OnDestroy {
  menuAbierto = false;
  usuario: Usuario | null;
  noLeidas = 0;
  private readonly subscriptions = new Subscription();

  constructor(
    public authService: AuthService,
    private notificacionService: NotificacionService,
    private router: Router
  ) {
    this.usuario = this.authService.getUsuario();
    this.subscriptions.add(this.authService.usuario$.subscribe((usuario) => {
      this.usuario = usuario;
    }));
  }

  ngOnInit(): void {
    this.subscriptions.add(this.notificacionService.noLeidas$.subscribe((count) => {
      this.noLeidas = count;
    }));

    this.subscriptions.add(timer(0, 20000).pipe(
      switchMap(() => this.notificacionService.actualizarConteo().pipe(
        catchError(() => EMPTY)
      ))
    ).subscribe());
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  get inicialUsuario(): string {
    return this.usuario?.name?.trim().charAt(0).toUpperCase() || 'J';
  }

  get etiquetaRol(): string {
    if (this.usuario?.role === 'dueno') return 'Dueño de cancha';
    if (this.usuario?.role === 'administrador') return 'Administrador';
    return 'Jugador';
  }

  alternarMenu(): void {
    this.menuAbierto = !this.menuAbierto;
  }

  cerrarMenu(): void {
    this.menuAbierto = false;
  }

  cerrarSesion(): void {
    this.authService.logout();
    this.notificacionService.limpiarConteo();
    this.router.navigate(['/login']);
  }
}
