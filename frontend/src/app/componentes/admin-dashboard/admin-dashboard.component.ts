import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService, AdminResumen, AdminPaginaUsuarios, AdminUsuarioDetalle, AdminPaginaCanchas, AdminCanchaDetalle } from '../../services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit {
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly resumen = signal<AdminResumen | null>(null);

  readonly usuarioSeleccionado = signal<AdminUsuarioDetalle | null>(null);
  readonly detalleCargando = signal(false);
  readonly detalleError = signal('');

  verUsuario(id: number): void {
    this.usuarioSeleccionado.set(null);
    this.detalleError.set('');
    this.detalleCargando.set(true);
    this.admin.detalleUsuario(id).subscribe({
      next: u => { this.usuarioSeleccionado.set(u); this.detalleCargando.set(false); },
      error: e => {
        this.detalleError.set(e.status === 404 ? 'El usuario ya no existe.' :
          e.status === 403 ? 'No tenés permiso para consultar esta cuenta.' : 'No se pudo cargar el detalle.');
        this.detalleCargando.set(false);
      }
    });
  }

  readonly estadoGuardando = signal(false);
  readonly estadoMensaje = signal('');
  readonly estadoError = signal('');
  motivoSuspension = '';

  cambiarEstadoUsuario(): void {
    const usuario = this.usuarioSeleccionado();
    if (!usuario || usuario.rol === 'administrador' || this.estadoGuardando()) return;
    const suspender = !usuario.suspendido;
    const motivo = this.motivoSuspension.trim();
    if (suspender && (motivo.length < 5 || motivo.length > 500)) {
      this.estadoError.set('Ingresá un motivo de 5 a 500 caracteres.'); return;
    }
    if (!window.confirm(suspender ? `¿Suspender a ${usuario.nombre}?` : `¿Reactivar a ${usuario.nombre}?`)) return;
    this.estadoGuardando.set(true);
    this.estadoError.set(''); this.estadoMensaje.set('');
    this.admin.cambiarEstadoUsuario(usuario.id, suspender, motivo).subscribe({
      next: actualizado => {
        this.usuarioSeleccionado.set(actualizado);
        this.motivoSuspension = '';
        this.estadoMensaje.set(suspender ? 'Cuenta suspendida.' : 'Cuenta reactivada.');
        this.estadoGuardando.set(false);
        this.cargarUsuarios(this.usuariosPagina()?.pagina ?? 0);
      },
      error: e => {
        this.estadoError.set(e.status === 403 ? 'Operación no permitida.' : e.status === 404 ? 'Cuenta no encontrada.' : 'No se pudo actualizar la cuenta.');
        this.estadoGuardando.set(false);
      }
    });
  }

  cerrarDetalle(): void {
    this.usuarioSeleccionado.set(null);
    this.detalleError.set('');
  }

  readonly canchaSeleccionada = signal<AdminCanchaDetalle | null>(null);
  readonly detalleCanchaCargando = signal(false);
  readonly detalleCanchaError = signal('');

  verDetalleCancha(id: number): void {
    this.canchaSeleccionada.set(null);
    this.detalleCanchaError.set('');
    this.estadoCanchaError.set(''); this.estadoCanchaMensaje.set('');
    this.detalleCanchaCargando.set(true);
    this.admin.detalleCanchaAdmin(id).subscribe({
      next: detalle => {
        this.canchaSeleccionada.set(detalle);
        this.detalleCanchaCargando.set(false);
      },
      error: err => {
        this.detalleCanchaError.set(err.status === 404 ? 'La cancha ya no existe.' :
          err.status === 403 ? 'No tenés permiso para consultar esta cancha.' :
          'No se pudo cargar el detalle de la cancha.');
        this.detalleCanchaCargando.set(false);
      }
    });
  }

  readonly estadoCanchaGuardando = signal(false);
  readonly estadoCanchaError = signal('');
  readonly estadoCanchaMensaje = signal('');
  motivoDesactivacionCancha = '';

  cambiarEstadoCancha(): void {
    const cancha = this.canchaSeleccionada();
    if (!cancha || this.estadoCanchaGuardando()) return;
    const activar = !cancha.activa;
    const motivo = this.motivoDesactivacionCancha.trim();
    if (!activar && (motivo.length < 5 || motivo.length > 500)) {
      this.estadoCanchaError.set('Ingresá un motivo de 5 a 500 caracteres.'); return;
    }
    if (!window.confirm(activar ? `¿Reactivar la cancha ${cancha.nombre}?` : `¿Desactivar la cancha ${cancha.nombre}? Las reservas existentes no se cancelarán.`)) return;
    this.estadoCanchaGuardando.set(true);
    this.estadoCanchaError.set(''); this.estadoCanchaMensaje.set('');
    this.admin.cambiarEstadoCancha(cancha.id, activar, motivo).subscribe({
      next: actualizada => {
        this.canchaSeleccionada.set(actualizada);
        this.motivoDesactivacionCancha = '';
        this.estadoCanchaMensaje.set(activar ? 'Cancha reactivada.' : 'Cancha desactivada. No se aceptan nuevas reservas.');
        this.estadoCanchaGuardando.set(false);
        this.admin.canchasAdmin(this.busquedaCanchas.trim(), this.estadoCanchas, this.canchasPagina()?.pagina ?? 0)
          .subscribe({ next: pagina => this.canchasPagina.set(pagina), error: () => this.canchasError.set('Actualizá la lista para ver el nuevo estado.') });
      },
      error: err => {
        this.estadoCanchaError.set(err.status === 403 ? 'Operación no autorizada.' :
          err.status === 404 ? 'Cancha no encontrada.' : err.status === 409 ? 'El estado ya fue modificado.' :
          'No se pudo cambiar el estado de la cancha.');
        this.estadoCanchaGuardando.set(false);
      }
    });
  }

  cerrarDetalleCancha(): void {
    this.canchaSeleccionada.set(null);
    this.detalleCanchaError.set('');
  }

  readonly canchasPagina = signal<AdminPaginaCanchas | null>(null);
  readonly canchasCargando = signal(false);
  readonly canchasError = signal('');
  busquedaCanchas = '';
  estadoCanchas = '';

  buscarCanchas(): void { this.cargarCanchas(0); }

  cargarCanchas(pagina = 0): void {
    this.cerrarDetalleCancha();
    this.canchasCargando.set(true);
    this.canchasError.set('');
    this.admin.canchasAdmin(this.busquedaCanchas.trim(), this.estadoCanchas, pagina).subscribe({
      next: datos => { this.canchasPagina.set(datos); this.canchasCargando.set(false); },
      error: err => {
        this.canchasError.set(err.status === 403 ? 'No tenés permiso para consultar canchas.' : 'No se pudieron cargar las canchas.');
        this.canchasCargando.set(false);
      }
    });
  }

  readonly usuariosPagina = signal<AdminPaginaUsuarios | null>(null);
  readonly usuariosCargando = signal(false);
  readonly usuariosError = signal('');
  busquedaUsuarios = '';
  rolUsuarios = '';

  constructor(private readonly admin: AdminService) {}

  buscarUsuarios(): void { this.cargarUsuarios(0); }

  cargarUsuarios(pagina = 0): void {
    this.usuariosCargando.set(true);
    this.usuariosError.set('');
    this.admin.usuarios(this.busquedaUsuarios.trim(), this.rolUsuarios, pagina).subscribe({
      next: datos => { this.usuariosPagina.set(datos); this.usuariosCargando.set(false); },
      error: err => {
        this.usuariosError.set(err.status === 403 ? 'No tenés permiso para consultar usuarios.' : 'No se pudieron cargar los usuarios.');
        this.usuariosCargando.set(false);
      }
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set('');
    this.resumen.set(null);

    this.admin.resumen().subscribe({
      next: (data) => {
        this.resumen.set(data);
        this.cargando.set(false);
      },
      error: (err) => {
        this.resumen.set(null);
        this.error.set(
          err.status === 403
            ? 'Tu cuenta no tiene autorización para consultar este panel.'
            : err.status === 401
              ? 'La sesión venció. Volvé a iniciar sesión.'
              : 'No se pudo cargar el panel. Revisá que Spring Boot esté funcionando.'
        );
        this.cargando.set(false);
      },
    });
  }
}
