import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { UbicacionPublicidadPublica } from './publicidades-publicas.service';

export interface TotalesPublicidad { impresiones: number; clics: number; ctr: number; }
export interface DiaPublicidad extends TotalesPublicidad { fecha: string; }
export interface AnuncioPublicidad extends TotalesPublicidad {
  id: number; titulo: string; ubicacion: string; estado: string;
}
export interface EstadisticasPublicidad {
  dias: number;
  ubicacion: UbicacionPublicidadPublica | null;
  publicidadId: number | null;
  desde: string; hasta: string;
  totales: TotalesPublicidad;
  evolucion: DiaPublicidad[];
  anuncios: AnuncioPublicidad[];
}

@Injectable({ providedIn: 'root' })
export class PublicidadesMetricasService {
  private readonly api = `${environment.apiUrl}/publicidades`;
  constructor(private readonly http: HttpClient) {}

  impresion(id: number, ubicacion: UbicacionPublicidadPublica): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/impresiones`, { ubicacion });
  }

  clic(id: number, ubicacion: UbicacionPublicidadPublica): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/clics`, { ubicacion });
  }

  estadisticas(dias: 7 | 30, ubicacion = '', publicidadId = ''): Observable<EstadisticasPublicidad> {
    let params = new HttpParams().set('dias', String(dias));
    if (ubicacion) params = params.set('ubicacion', ubicacion);
    if (publicidadId.trim()) params = params.set('publicidadId', publicidadId.trim());
    return this.http.get<EstadisticasPublicidad>(`${environment.apiUrl}/admin/publicidades/estadisticas`, { params });
  }
}
