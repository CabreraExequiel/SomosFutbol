import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService, AdminResumen, AdminPaginaUsuarios, AdminUsuarioDetalle } from '../../services/admin.service';

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
