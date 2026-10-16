import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Disponibilidad } from '../models/agenda';
import { environment } from '../../environments/environment';

export interface DatosTransferencia {
  reservaId: number;
  canchaNombre: string;
  duenoNombre: string;
  monto: number;
  cbuCvu: string | null;
  aliasMercadoPago: string | null;
}

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  disponibilidad(canchaId: number, fecha: string): Observable<Disponibilidad> {
    const params = new HttpParams()
      .set('canchaId', canchaId)
      .set('fecha', fecha);
    return this.http.get<Disponibilidad>(`${this.apiUrl}/reservas/disponibilidad`, { params });
  }

  detalle(id:number): Observable<any> { return this.http.get(`${this.apiUrl}/reservas/${id}/detalle`); }

  misReservas(): Observable<any> {
    return this.http.get(`${this.apiUrl}/reservas`);
  }

  reservasDeMisCanchas(): Observable<any> {
    return this.http.get(`${this.apiUrl}/reservas/recibidas`);
  }

  crear(data: { canchaId: number; fecha: string; horaInicio: string; horaFin: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/reservas`, data);
  }

  cancelar(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/reservas/${id}/cancelar`, {});
  }

  confirmar(id: number): Observable<any> {
    return this.http.put(`${this.apiUrl}/reservas/${id}/confirmar`, {});
  }

  datosTransferencia(id: number): Observable<DatosTransferencia> {
    return this.http.get<DatosTransferencia>(`${this.apiUrl}/reservas/${id}/datos-transferencia`);
  }
}
