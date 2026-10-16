import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Horario, Bloqueo, DuenoPublico } from '../models/agenda';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class CanchaService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  listar(filtros: { ciudadId?: number; tipo?: string } = {}): Observable<any> {
    let params = new HttpParams();
    if (filtros.ciudadId) params = params.set('ciudadId', filtros.ciudadId);
    if (filtros.tipo) params = params.set('tipo', filtros.tipo);
    return this.http.get(`${this.apiUrl}/canchas`, { params });
  }

  obtener(id: number): Observable<any> {
    return this.http.get(`${this.apiUrl}/canchas/${id}`);
  }

  misCanchas(): Observable<any> {
    return this.http.get(`${this.apiUrl}/canchas/mias/todas`);
  }

  crear(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/canchas`, data);
  }

  subirImagen(id: number, archivo: File): Observable<any> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post(`${this.apiUrl}/canchas/${id}/imagen`, formData);
  }

  actualizar(id: number, data: any): Observable<any> {
    return this.http.put(`${this.apiUrl}/canchas/${id}`, data);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/canchas/${id}`);
  }

  agregarHorario(canchaId: number, data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/canchas/${canchaId}/horarios`, data);
  }

  eliminarHorario(canchaId: number, horarioId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/canchas/${canchaId}/horarios/${horarioId}`);
  }

  duenos(filtros: {ciudadId?: number; tipo?: string} = {}): Observable<DuenoPublico[]> {
    let params=new HttpParams();
    if(filtros.ciudadId) params=params.set('ciudadId',filtros.ciudadId);
    if(filtros.tipo) params=params.set('tipo',filtros.tipo);
    return this.http.get<DuenoPublico[]>(`${this.apiUrl}/canchas/duenos`,{params});
  }
  porDueno(id:number): Observable<any[]> { return this.http.get<any[]>(`${this.apiUrl}/canchas/duenos/${id}`); }
  publicar(id:number,activa:boolean): Observable<any> {return this.http.patch(`${this.apiUrl}/canchas/${id}/publicacion`,{activa});}
  horarios(id:number): Observable<Horario[]> {return this.http.get<Horario[]>(`${this.apiUrl}/canchas/${id}/horarios`);}
  guardarHorarios(id:number,horarios:Horario[]): Observable<Horario[]> {return this.http.put<Horario[]>(`${this.apiUrl}/canchas/${id}/horarios`,{horarios});}
  bloqueos(id:number): Observable<Bloqueo[]> {return this.http.get<Bloqueo[]>(`${this.apiUrl}/canchas/${id}/bloqueos`);}
  bloquear(id:number,datos:any): Observable<Bloqueo> {return this.http.post<Bloqueo>(`${this.apiUrl}/canchas/${id}/bloqueos`,datos);}
  desbloquear(id:number,bloqueo:number): Observable<void> {return this.http.delete<void>(`${this.apiUrl}/canchas/${id}/bloqueos/${bloqueo}`);}

  listarCiudades(): Observable<any> {
    return this.http.get(`${this.apiUrl}/ciudades`);
  }
}
