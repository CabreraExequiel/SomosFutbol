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
  suspendido: boolean;
  motivoSuspension: string | null;
}

export interface AdminPaginaUsuarios {
  usuarios: AdminUsuarioResumen[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

export interface AdminCanchaResumen {
  id: number;
  nombre: string;
  complejo: string;
  ciudad: string;
  propietario: string;
  tipo: string | null;
  activa: boolean;
  complejoSuspendido: boolean;
}

export interface AdminCanchaDetalle extends AdminCanchaResumen {
  propietarioId: number;
  superficie: string | null;
  cantJugadores: number | null;
  precioHora: number;
  techada: boolean;
  direccion: string | null;
  descripcion: string | null;
  motivoDesactivacionAdmin: string | null;
}

export interface AdminComplejoDetalle {
  propietarioId: number;
  nombreComplejo: string;
  propietario: string;
  suspendido: boolean;
  motivoSuspension: string | null;
  canchasTotal: number;
  canchasActivas: number;
}

export interface AdminPaginaCanchas {
  canchas: AdminCanchaResumen[];
  pagina: number;
  totalPaginas: number;
  total: number;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  detalleComplejoAdmin(propietarioId: number): Observable<AdminComplejoDetalle> {
    return this.http.get<AdminComplejoDetalle>(`${environment.apiUrl}/admin/complejos/${propietarioId}`);
  }

  cambiarEstadoComplejo(propietarioId: number, suspendido: boolean, motivo: string): Observable<AdminComplejoDetalle> {
    return this.http.post<AdminComplejoDetalle>(`${environment.apiUrl}/admin/complejos/${propietarioId}/estado`, { suspendido, motivo });
  }

  cambiarEstadoCancha(id: number, activa: boolean, motivo: string): Observable<AdminCanchaDetalle> {
    return this.http.post<AdminCanchaDetalle>(`${environment.apiUrl}/admin/canchas/${id}/estado`, { activa, motivo });
  }

  detalleCanchaAdmin(id: number): Observable<AdminCanchaDetalle> {
    return this.http.get<AdminCanchaDetalle>(`${environment.apiUrl}/admin/canchas/${id}`);
  }

  canchasAdmin(buscar = '', activa = '', pagina = 0): Observable<AdminPaginaCanchas> {
    const params: Record<string, string> = { buscar, pagina: String(pagina) };
    if (activa !== '') params['activa'] = activa;
    return this.http.get<AdminPaginaCanchas>(`${environment.apiUrl}/admin/canchas`, { params });
  }


  usuarios(buscar = '', rol = '', pagina = 0): Observable<AdminPaginaUsuarios> {
    const params: Record<string, string> = { buscar, pagina: String(pagina) };
    if (rol) params['rol'] = rol;
    return this.http.get<AdminPaginaUsuarios>(`${environment.apiUrl}/admin/usuarios`, { params });
  }

  detalleUsuario(id: number): Observable<AdminUsuarioDetalle> {
    return this.http.get<AdminUsuarioDetalle>(`${environment.apiUrl}/admin/usuarios/${id}`);
  }

  cambiarEstadoUsuario(id: number, suspendido: boolean, motivo: string): Observable<AdminUsuarioDetalle> {
    return this.http.post<AdminUsuarioDetalle>(`${environment.apiUrl}/admin/usuarios/${id}/estado`, { suspendido, motivo });
  }

  resumen(): Observable<AdminResumen> {
    return this.http.get<AdminResumen>(`${environment.apiUrl}/admin/resumen`);
  }
}
