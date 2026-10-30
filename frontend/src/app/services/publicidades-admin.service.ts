import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type EstadoPublicidad = 'BORRADOR' | 'ACTIVA' | 'PAUSADA' | 'ARCHIVADA';
export type UbicacionPublicidad = 'INICIO' | 'FEED' | 'BUSCAR_CANCHAS';

export interface PublicidadAdmin {
  id: number;
  titulo: string;
  descripcion: string | null;
  imagenUrl: string | null;
  destinoUrl: string;
  ubicacion: UbicacionPublicidad;
  estado: EstadoPublicidad;
  prioridad: number;
  fechaInicio: string | null;
  fechaFin: string | null;
  creadoEn: string;
  actualizadoEn: string;
  creadoPorId: number;
  actualizadoPorId: number;
}

export interface PublicidadSolicitud {
  titulo: string;
  descripcion: string | null;
  imagenUrl: string | null;
  destinoUrl: string;
  ubicacion: UbicacionPublicidad;
  prioridad: number;
  fechaInicio: string | null;
  fechaFin: string | null;
}

export interface PaginaPublicidades {
  content: PublicidadAdmin[];
  number: number;
  totalPages: number;
  totalElements: number;
  first: boolean;
  last: boolean;
}

@Injectable({ providedIn: 'root' })
export class PublicidadesAdminService {
  private readonly url = `${environment.apiUrl}/admin/publicidades`;
  constructor(private readonly http: HttpClient) {}

  listar(buscar: string, estado: string, ubicacion: string, pagina: number): Observable<PaginaPublicidades> {
    let params = new HttpParams()
      .set('buscar', buscar)
      .set('pagina', pagina.toString());
    if (estado) params = params.set('estado', estado);
    if (ubicacion) params = params.set('ubicacion', ubicacion);
    return this.http.get<PaginaPublicidades>(this.url, { params });
  }

  detalle(id: number): Observable<PublicidadAdmin> {
    return this.http.get<PublicidadAdmin>(`${this.url}/${id}`);
  }

  crear(solicitud: PublicidadSolicitud): Observable<PublicidadAdmin> {
    return this.http.post<PublicidadAdmin>(this.url, solicitud);
  }

  editar(id: number, solicitud: PublicidadSolicitud): Observable<PublicidadAdmin> {
    return this.http.put<PublicidadAdmin>(`${this.url}/${id}`, solicitud);
  }

  cambiarEstado(id: number, estado: EstadoPublicidad): Observable<PublicidadAdmin> {
    return this.http.patch<PublicidadAdmin>(`${this.url}/${id}/estado`, { estado });
  }
}
