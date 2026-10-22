import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type EstadoIncidente = 'PENDIENTE' | 'EN_INVESTIGACION' | 'RESUELTO';

export interface EventoBitacora {
  id: number;
  fecha: string;
  nivel: 'ERROR' | 'INFO';
  origen: 'BACKEND' | 'FRONTEND' | 'AUDITORIA';
  ambiente: string;
  tipo: string;
  codigo: string;
  descripcion: string;
  actorId: number | null;
  referenciaTipo: string | null;
  referenciaId: number | null;
  ruta: string | null;
  claseOrigen: string | null;
  funcionOrigen: string | null;
  archivoOrigen: string | null;
  lineaOrigen: number | null;
  causaProbable: string | null;
  sqlState: string | null;
  codigoSql: number | null;
  estado: EstadoIncidente;
  atendidoPorId: number | null;
  atendidoEn: string | null;
}
export interface PaginaBitacora {
  content: EventoBitacora[];
  number: number;
  totalPages: number;
  totalElements: number;
}
@Injectable({ providedIn: 'root' })
export class BitacoraService {
  constructor(private readonly http: HttpClient) {}
  actualizarEstado(id: number, estado: EstadoIncidente): Observable<EventoBitacora> {
    return this.http.patch<EventoBitacora>(`${environment.apiUrl}/admin/bitacora/${id}/estado`, { estado });
  }
  listar(pagina = 0, nivel = '', origen = ''): Observable<PaginaBitacora> {
    let params = new HttpParams().set('pagina', pagina);
    if (nivel) params = params.set('nivel', nivel);
    if (origen) params = params.set('origen', origen);
    return this.http.get<PaginaBitacora>(`${environment.apiUrl}/admin/bitacora`, { params });
  }
}
