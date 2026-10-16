import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export type TipoNotificacion =
  | 'SOLICITUD_AMISTAD'
  | 'AMISTAD_ACEPTADA'
  | 'MENSAJE'
  | 'ETIQUETA'
  | 'COMENTARIO'
  | 'ME_GUSTA'
  | 'RESERVA'
  | 'PAGO';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  destino?: string;
  referenciaId?: number;
  leida: boolean;
  createdAt: string;
  actorId?: number;
  actorNombre?: string;
  actorAvatar?: string;
}

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly apiUrl = `${environment.apiUrl}/notificaciones`;
  private readonly noLeidasSubject = new BehaviorSubject<number>(0);

  readonly noLeidas$ = this.noLeidasSubject.asObservable();

  constructor(private http: HttpClient) {}

  listar(limit = 50): Observable<Notificacion[]> {
    const params = new HttpParams().set('limit', limit);
    return this.http.get<Notificacion[]>(this.apiUrl, { params });
  }

  actualizarConteo(): Observable<number> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/no-leidas/count`).pipe(
      map((respuesta) => respuesta.count),
      tap((count) => this.noLeidasSubject.next(count))
    );
  }

  marcarLeida(id: number): Observable<Notificacion> {
    return this.http.patch<Notificacion>(`${this.apiUrl}/${id}/leer`, {}).pipe(
      tap((notificacion) => {
        if (notificacion.leida) {
          this.noLeidasSubject.next(Math.max(0, this.noLeidasSubject.value - 1));
        }
      })
    );
  }

  marcarTodas(): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/leer-todas`, {}).pipe(
      tap(() => this.noLeidasSubject.next(0))
    );
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  limpiarConteo(): void {
    this.noLeidasSubject.next(0);
  }
}
