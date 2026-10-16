import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Notificacion, NotificacionService, TipoNotificacion } from '../../services/notificacion.service';

@Component({
    selector: 'app-notificaciones',
    imports: [CommonModule],
    templateUrl: './notificaciones.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './notificaciones.component.css'
})
export class NotificacionesComponent implements OnInit {
  notificaciones: Notificacion[] = [];
  tipoFiltro='';
  filtro: 'todas' | 'no-leidas' = 'todas';
  cargando = true;
  procesando = false;
  error = '';

  constructor(
    private notificacionService: NotificacionService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.notificacionService.listar().subscribe({
      next: (notificaciones) => {
        this.notificaciones = notificaciones;
        this.cargando = false;
        this.notificacionService.actualizarConteo().subscribe({ error: () => undefined });
      },
      error: () => {
        this.error = 'No pudimos cargar tus notificaciones.';
        this.cargando = false;
      },
    });
  }

  abrir(notificacion: Notificacion): void {
    const navegar = (): void => {
      if (notificacion.destino) void this.router.navigateByUrl(notificacion.destino);
    };

    if (notificacion.leida) {
      navegar();
      return;
    }

    this.notificacionService.marcarLeida(notificacion.id).subscribe({
      next: (actualizada) => {
        Object.assign(notificacion, actualizada);
        navegar();
      },
      error: navegar,
    });
  }

  marcarTodas(): void {
    if (this.noLeidas === 0) return;
    this.procesando = true;
    this.notificacionService.marcarTodas().subscribe({
      next: () => {
        this.notificaciones = this.notificaciones.map((item) => ({ ...item, leida: true }));
        this.procesando = false;
      },
      error: () => {
        this.error = 'No se pudieron marcar las notificaciones.';
        this.procesando = false;
      },
    });
  }

  eliminar(event: Event, notificacion: Notificacion): void {
    event.stopPropagation();
    this.notificacionService.eliminar(notificacion.id).subscribe({
      next: () => {
        this.notificaciones = this.notificaciones.filter((item) => item.id !== notificacion.id);
        if (!notificacion.leida) {
          this.notificacionService.actualizarConteo().subscribe({ error: () => undefined });
        }
      },
      error: () => (this.error = 'No se pudo eliminar la notificación.'),
    });
  }

  icono(tipo: TipoNotificacion): string {
    const iconos: Record<TipoNotificacion, string> = {
      SOLICITUD_AMISTAD: '👤',
      AMISTAD_ACEPTADA: '✓',
      MENSAJE: '✉',
      ETIQUETA: '@',
      COMENTARIO: '💬',
      ME_GUSTA: '⚽',
      RESERVA: '📅',
      PAGO: '$',
    };
    return iconos[tipo];
  }

  get visibles(): Notificacion[] {
    return this.notificaciones.filter(n=>(this.filtro!=='no-leidas'||!n.leida)&&(!this.tipoFiltro||n.tipo===this.tipoFiltro));
  }

  get noLeidas(): number {
    return this.notificaciones.filter((item) => !item.leida).length;
  }
}
