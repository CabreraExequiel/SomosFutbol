import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'REEMBOLSADO';

export interface PagoResponse {
  id: number;
  reservaId: number;
  monto: number;
  estado: EstadoPago;
  initPoint: string | null;
  mpPaymentId: string | null;
  fechaPago: string | null;
}

@Injectable({ providedIn: 'root' })
export class PagoService {
  private readonly apiUrl = `${environment.apiUrl}/pagos`;

  constructor(private http: HttpClient) {}

  crearPreferencia(reservaId: number): Observable<PagoResponse> {
    return this.http.post<PagoResponse>(
      `${this.apiUrl}/reservas/${reservaId}/preferencia`,
      {}
    );
  }

  obtenerPorReserva(reservaId: number): Observable<PagoResponse> {
    return this.http.get<PagoResponse>(`${this.apiUrl}/reservas/${reservaId}`);
  }
}
