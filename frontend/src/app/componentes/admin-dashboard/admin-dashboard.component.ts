import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AdminService, AdminResumen, AdminPaginaUsuarios } from '../../services/admin.service';

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
