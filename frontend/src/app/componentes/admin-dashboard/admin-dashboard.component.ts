import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminPublicidadesComponent } from '../admin-publicidades/admin-publicidades.component';
import { AdminService, AdminResumen, AdminPaginaUsuarios, AdminUsuarioDetalle, AdminPaginaCanchas, AdminCanchaDetalle, AdminComplejoDetalle, AdminHistorialPagina, AdminPaginaReservas, AdminReservaDetalle, AdminPaginaPagos, AdminResumenConciliacion, AdminPaginaCasosConciliacion, AdminTipoCasoConciliacion } from '../../services/admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, AdminPublicidadesComponent],
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

  readonly historialCancha = signal<AdminHistorialPagina | null>(null);
  readonly historialCanchaCargando = signal(false);
  readonly historialCanchaError = signal('');

  cargarHistorialCancha(pagina = 0): void {
    const cancha = this.canchaSeleccionada();
    if (!cancha) return;
    this.historialCanchaCargando.set(true);
    this.historialCanchaError.set('');
    this.admin.historialCancha(cancha.id, pagina).subscribe({
      next: resultado => {
        if (this.canchaSeleccionada()?.id !== cancha.id) return;
        this.historialCancha.set(resultado);
        this.historialCanchaCargando.set(false);
      },
      error: () => {
        if (this.canchaSeleccionada()?.id !== cancha.id) return;
        this.historialCanchaError.set('No se pudo consultar el historial de la cancha.');
        this.historialCanchaCargando.set(false);
      },
    });
  }

  readonly canchaSeleccionada = signal<AdminCanchaDetalle | null>(null);
  readonly detalleCanchaCargando = signal(false);
  readonly detalleCanchaError = signal('');

  verDetalleCancha(id: number): void {
    this.canchaSeleccionada.set(null);
    this.historialCancha.set(null);
    this.historialCanchaError.set('');
    this.detalleCanchaError.set('');
    this.estadoCanchaError.set(''); this.estadoCanchaMensaje.set('');
    this.cerrarDetalleComplejo();
    this.detalleCanchaCargando.set(true);
    this.admin.detalleCanchaAdmin(id).subscribe({
      next: detalle => {
        this.canchaSeleccionada.set(detalle);
        this.cargarHistorialCancha();
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
        this.cargarHistorialCancha();
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
    this.historialCancha.set(null);
    this.historialCanchaError.set('');
    this.detalleCanchaError.set('');
    this.cerrarDetalleComplejo();
  }

  readonly historialComplejo = signal<AdminHistorialPagina | null>(null);
  readonly historialComplejoCargando = signal(false);
  readonly historialComplejoError = signal('');

  cargarHistorialComplejo(pagina = 0): void {
    const complejo = this.complejoSeleccionado();
    if (!complejo) return;
    this.historialComplejoCargando.set(true);
    this.historialComplejoError.set('');
    this.admin.historialComplejo(complejo.propietarioId, pagina).subscribe({
      next: resultado => {
        if (this.complejoSeleccionado()?.propietarioId !== complejo.propietarioId) return;
        this.historialComplejo.set(resultado);
        this.historialComplejoCargando.set(false);
      },
      error: () => {
        if (this.complejoSeleccionado()?.propietarioId !== complejo.propietarioId) return;
        this.historialComplejoError.set('No se pudo consultar el historial del complejo.');
        this.historialComplejoCargando.set(false);
      },
    });
  }

  readonly complejoSeleccionado = signal<AdminComplejoDetalle | null>(null);
  readonly complejoCargando = signal(false);
  readonly complejoError = signal('');
  readonly estadoComplejoGuardando = signal(false);
  readonly estadoComplejoError = signal('');
  readonly estadoComplejoMensaje = signal('');
  motivoSuspensionComplejo = '';

  verDetalleComplejo(propietarioId: number): void {
    this.cerrarDetalleComplejo();
    this.complejoCargando.set(true);
    this.admin.detalleComplejoAdmin(propietarioId).subscribe({
      next: complejo => {
        this.complejoSeleccionado.set(complejo);
        this.cargarHistorialComplejo();
        this.complejoCargando.set(false);
      },
      error: err => {
        this.complejoError.set(err.status === 404 ? 'Propietario no encontrado.' :
          err.status === 403 ? 'No tenés permisos.' : 'No se pudo cargar el complejo.');
        this.complejoCargando.set(false);
      }
    });
  }

  cambiarEstadoComplejo(): void {
    const complejo = this.complejoSeleccionado();
    if (!complejo || this.estadoComplejoGuardando()) return;
    const suspender = !complejo.suspendido;
    const motivo = this.motivoSuspensionComplejo.trim();
    if (suspender && (motivo.length < 5 || motivo.length > 500)) {
      this.estadoComplejoError.set('Ingresá un motivo de 5 a 500 caracteres.'); return;
    }
    const mensaje = suspender
      ? `¿Suspender ${complejo.nombreComplejo} y bloquear nuevas reservas de sus ${complejo.canchasTotal} canchas? Las reservas existentes se conservan.`
      : `¿Reactivar ${complejo.nombreComplejo}? Las canchas individuales conservarán su propio estado.`;
    if (!window.confirm(mensaje)) return;
    this.estadoComplejoGuardando.set(true);
    this.estadoComplejoError.set(''); this.estadoComplejoMensaje.set('');
    this.admin.cambiarEstadoComplejo(complejo.propietarioId, suspender, motivo).subscribe({
      next: actualizado => {
        this.complejoSeleccionado.set(actualizado);
        this.cargarHistorialComplejo();
        this.motivoSuspensionComplejo = '';
        this.estadoComplejoMensaje.set(suspender ? 'Complejo suspendido: no se permiten nuevas reservas.' :
          'Complejo reactivado. Se respetaron los estados de cada cancha.');
        this.estadoComplejoGuardando.set(false);
        const cancha = this.canchaSeleccionada();
        if (cancha?.propietarioId === actualizado.propietarioId) {
          this.canchaSeleccionada.set({ ...cancha, complejoSuspendido: actualizado.suspendido });
        }
        this.admin.canchasAdmin(this.busquedaCanchas.trim(), this.estadoCanchas, this.canchasPagina()?.pagina ?? 0)
          .subscribe({ next: pagina => this.canchasPagina.set(pagina), error: () => this.canchasError.set('Actualizá el listado de canchas.') });
      },
      error: err => {
        this.estadoComplejoError.set(err.status === 409 ? 'El estado cambió o esta cuenta no es de un dueño.' :
          err.status === 404 ? 'Propietario no encontrado.' :
          err.status === 403 ? 'No tenés permisos.' : 'No se pudo cambiar el estado del complejo.');
        this.estadoComplejoGuardando.set(false);
      }
    });
  }

  cerrarDetalleComplejo(): void {
    this.complejoSeleccionado.set(null);
    this.historialComplejo.set(null);
    this.historialComplejoError.set('');
    this.complejoError.set('');
    this.estadoComplejoError.set(''); this.estadoComplejoMensaje.set('');
    this.motivoSuspensionComplejo = '';
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

  // 09D4: alertas de conciliación de solo lectura; cada caso requiere verificación humana.
  readonly conciliacionResumen = signal<AdminResumenConciliacion | null>(null);
  readonly conciliacionResumenCargando = signal(false);
  readonly conciliacionResumenError = signal('');
  readonly conciliacionPagina = signal<AdminPaginaCasosConciliacion | null>(null);
  readonly conciliacionCargando = signal(false);
  readonly conciliacionError = signal('');
  tipoConciliacion: AdminTipoCasoConciliacion = 'CONFIRMADA_SIN_PAGO_APROBADO';

  cargarConciliacionResumen(): void {
    this.conciliacionResumenCargando.set(true);
    this.conciliacionResumenError.set('');
    this.admin.resumenConciliacion().subscribe({
      next: resumen => { this.conciliacionResumen.set(resumen); this.conciliacionResumenCargando.set(false); },
      error: err => {
        this.conciliacionResumenError.set(err.status === 403 ? 'No tenés permiso para consultar alertas.' :
          'No se pudieron cargar los indicadores de conciliación.');
        this.conciliacionResumenCargando.set(false);
      }
    });
  }

  cargarConciliacion(pagina = 0): void {
    const tipo = this.tipoConciliacion;
    this.conciliacionCargando.set(true);
    this.conciliacionError.set('');
    this.conciliacionPagina.set(null);
    this.admin.casosConciliacion(tipo, pagina).subscribe({
      next: resultado => {
        if (this.tipoConciliacion !== tipo) return;
        this.conciliacionPagina.set(resultado);
        this.conciliacionCargando.set(false);
      },
      error: err => {
        if (this.tipoConciliacion !== tipo) return;
        this.conciliacionError.set(err.status === 403 ? 'No tenés permiso para consultar estos casos.' :
          err.status === 401 ? 'La sesión venció o falta autorización.' :
          `No se pudieron consultar los casos (HTTP ${err.status || 'sin respuesta'}).`);
        this.conciliacionCargando.set(false);
      }
    });
  }

  actualizarConciliacion(): void {
    this.cargarConciliacionResumen();
    this.cargarConciliacion(0);
  }

  // 09D3: búsqueda paginada de pagos locales, sin operaciones de escritura.
  readonly pagosPagina = signal<AdminPaginaPagos | null>(null);
  readonly pagosCargando = signal(false);
  readonly pagosError = signal('');
  busquedaPagos = '';
  estadoPagos = '';

  buscarPagos(): void { this.cargarPagos(0); }

  cargarPagos(pagina = 0): void {
    this.pagosCargando.set(true);
    this.pagosError.set('');
    this.admin.pagosAdmin(this.busquedaPagos.trim(), this.estadoPagos, pagina).subscribe({
      next: datos => { this.pagosPagina.set(datos); this.pagosCargando.set(false); },
      error: err => {
        this.pagosError.set(err.status === 403 ? 'No tenés permiso para consultar pagos.' :
          'No se pudieron cargar los pagos registrados. Revisá Spring Boot.');
        this.pagosCargando.set(false);
      }
    });
  }

  // 09D2: detalle de reserva y estado de pago registrado, sin acciones de modificación.
  readonly reservaSeleccionada = signal<AdminReservaDetalle | null>(null);
  readonly detalleReservaCargando = signal(false);
  readonly detalleReservaError = signal('');
  private reservaSolicitadaId: number | null = null;

  verDetalleReserva(id: number): void {
    this.reservaSolicitadaId = id;
    this.reservaSeleccionada.set(null);
    this.detalleReservaError.set('');
    this.detalleReservaCargando.set(true);
    this.admin.detalleReservaAdmin(id).subscribe({
      next: detalle => {
        if (this.reservaSolicitadaId !== id) return;
        this.reservaSeleccionada.set(detalle);
        this.detalleReservaCargando.set(false);
      },
      error: err => {
        if (this.reservaSolicitadaId !== id) return;
        this.detalleReservaError.set(err.status === 404 ? 'La reserva ya no existe.' :
          err.status === 403 ? 'No tenés permiso para consultar esta reserva.' :
          'No se pudo cargar el detalle de la reserva.');
        this.detalleReservaCargando.set(false);
      },
    });
  }

  cerrarDetalleReserva(): void {
    this.reservaSolicitadaId = null;
    this.reservaSeleccionada.set(null);
    this.detalleReservaCargando.set(false);
    this.detalleReservaError.set('');
  }

  // 09D1: listado paginado de reservas; sin operaciones que alteren reservas o pagos.
  readonly reservasPagina = signal<AdminPaginaReservas | null>(null);
  readonly reservasCargando = signal(false);
  readonly reservasError = signal('');
  busquedaReservas = '';
  estadoReservas = '';

  buscarReservas(): void { this.cargarReservas(0); }

  cargarReservas(pagina = 0): void {
    this.cerrarDetalleReserva();
    this.reservasCargando.set(true);
    this.reservasError.set('');
    this.admin.reservasAdmin(this.busquedaReservas.trim(), this.estadoReservas, pagina).subscribe({
      next: datos => { this.reservasPagina.set(datos); this.reservasCargando.set(false); },
      error: err => {
        this.reservasError.set(err.status === 403 ? 'No tenés permiso para consultar reservas.' :
          'No se pudieron cargar las reservas. Revisá Spring Boot.');
        this.reservasCargando.set(false);
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
    this.cargarReservas();
    this.cargarPagos();
    this.actualizarConciliacion();
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
