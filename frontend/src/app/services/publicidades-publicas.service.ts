import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type UbicacionPublicidadPublica = 'INICIO' | 'FEED' | 'BUSCAR_CANCHAS';

/** Respuesta de consulta pública: nunca incluye campos de administración. */
export interface PublicidadVisible {
  id: number;
  titulo: string;
  descripcion: string | null;
  imagenUrl: string | null;
  destinoUrl: string;
  ubicacion: UbicacionPublicidadPublica;
}

@Injectable({ providedIn: 'root' })
export class PublicidadesPublicasService {
  private readonly endpoint = `${environment.apiUrl}/publicidades/visibles`;
  constructor(private readonly http: HttpClient) {}

  listar(ubicacion: UbicacionPublicidadPublica): Observable<PublicidadVisible[]> {
    return this.http.get<PublicidadVisible[]>(this.endpoint, {
      params: new HttpParams().set('ubicacion', ubicacion)
    });
  }
}
