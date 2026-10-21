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
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  constructor(private http: HttpClient) {}

  resumen(): Observable<AdminResumen> {
    return this.http.get<AdminResumen>(`${environment.apiUrl}/admin/resumen`);
  }
}
