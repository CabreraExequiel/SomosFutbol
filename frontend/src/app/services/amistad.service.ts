import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Amistad {
  id: number;
  estado: 'PENDIENTE' | 'ACEPTADA' | 'RECHAZADA';
  userId: number;
  nombre: string;
  avatar?: string;
  ciudad?: string;
  enviadaPorMi: boolean;
  createdAt: string;
  respondedAt?: string;
}

export interface UsuarioSocial {
  id: number;
  name: string;
  avatar?: string;
  ciudad?: string;
  role: 'usuario' | 'dueno' | 'administrador';
  relacion: 'NINGUNA' | 'PENDIENTE_ENVIADA' | 'PENDIENTE_RECIBIDA' | 'AMIGOS';
  solicitudId?: number;
}

@Injectable({ providedIn: 'root' })
export class AmistadService {
  private readonly apiUrl = `${environment.apiUrl}/amistades`;

  constructor(private http: HttpClient) {}

  listarAmigos(): Observable<Amistad[]> {
    return this.http.get<Amistad[]>(this.apiUrl);
  }

  solicitudesRecibidas(): Observable<Amistad[]> {
    return this.http.get<Amistad[]>(`${this.apiUrl}/solicitudes/recibidas`);
  }

  solicitudesEnviadas(): Observable<Amistad[]> {
    return this.http.get<Amistad[]>(`${this.apiUrl}/solicitudes/enviadas`);
  }

  buscarUsuarios(query: string): Observable<UsuarioSocial[]> {
    const params = new HttpParams().set('q', query.trim());
    return this.http.get<UsuarioSocial[]>(`${this.apiUrl}/usuarios`, { params });
  }

  enviarSolicitud(userId: number): Observable<Amistad> {
    return this.http.post<Amistad>(`${this.apiUrl}/solicitudes/${userId}`, {});
  }

  aceptar(solicitudId: number): Observable<Amistad> {
    return this.http.patch<Amistad>(`${this.apiUrl}/solicitudes/${solicitudId}/aceptar`, {});
  }

  rechazar(solicitudId: number): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/solicitudes/${solicitudId}/rechazar`, {});
  }

  eliminarAmigo(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${userId}`);
  }
}
