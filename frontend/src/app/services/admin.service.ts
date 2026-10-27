import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AdminUsuarioResumen {
  id: number;
  nombre: string;
  email: string;
  rol: 'usuario' | 'dueno' | 'administrador';
}

export interface AdminResumen {
  usuarios: number;
  canchas: number;
  reservas: number;
  publicaciones: number;
  ultimosUsuarios: AdminUsuarioResumen[];
  canchasActivas: number;
  reservasPendientes: number;
  reservasConfirmadas: number;
  reservasCanceladas: number;
  pagosTotales: number;
  pagosPendientes: number;
  pagosAprobados: number;
  pagosRechazados: number;
  pagosReembolsados: number;
}

export interface AdminUsuarioDetalle extends AdminUsuarioResumen {
  ciudad: string | null;
  nombreComplejo: string | null;
  googleVinculado: boolean;
}

export interface AdminPaginaUsuarios {
  usuarios: AdminUsuarioResumen[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  usuarios(buscar = '', rol = '', pagina = 0): Observable<AdminPaginaUsuarios> {
    const params: Record<string, string> = { buscar, pagina: String(pagina) };
    if (rol) params['rol'] = rol;
    return this.http.get<AdminPaginaUsuarios>(`${environment.apiUrl}/admin/usuarios`, { params });
  }

  detalleUsuario(id: number): Observable<AdminUsuarioDetalle> {
    return this.http.get<AdminUsuarioDetalle>(`${environment.apiUrl}/admin/usuarios/${id}`);
  }

  resumen(): Observable<AdminResumen> {
    return this.http.get<AdminResumen>(`${environment.apiUrl}/admin/resumen`);
  }
}
