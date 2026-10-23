import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Usuario {
  id: number;
  name: string;
  email: string;
  telefono?: string;
  ciudad?: string;
  role: 'usuario' | 'dueno' | 'administrador';
  avatar?: string;
  nombreComplejo?: string;
}

export interface RegistroRequest {
  name: string;
  email: string;
  password: string;
  telefono: string;
  ciudad?: string;
  role: 'usuario' | 'dueno';
  cbuCvu?: string;
  aliasMercadoPago?: string;
  aceptaTerminos: boolean;
  aceptaPrivacidad: boolean;
}

export interface CuentaCobro {
  cbuCvu: string | null;
  aliasMercadoPago: string | null;
  configurada: boolean;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private authApiUrl = `${environment.apiUrl}/auth`;
  private usersApiUrl = `${environment.apiUrl}/users`;
  private usuarioSubject = new BehaviorSubject<Usuario | null>(this.leerUsuarioGuardado());

  readonly usuario$ = this.usuarioSubject.asObservable();

  constructor(private http: HttpClient) {}

  register(data: RegistroRequest): Observable<any> {
    return this.http.post(`${this.authApiUrl}/register`, data).pipe(
      tap((res: any) => this.guardarSesion(res))
    );
  }

  loginGoogle(credential: string, datos?: {
    telefono?: string;
    ciudad?: string;
    role?: 'usuario' | 'dueno';
    aceptaTerminos?: boolean;
    aceptaPrivacidad?: boolean;
  }): Observable<any> {
    return this.http.post(`${this.authApiUrl}/google`, { credential, ...datos }).pipe(
      tap((res: any) => this.guardarSesion(res))
    );
  }

  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.authApiUrl}/login`, { email, password }).pipe(
      tap((res: any) => this.guardarSesion(res))
    );
  }

  obtenerPerfil(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.usersApiUrl}/me`).pipe(
      tap((usuario) => this.guardarUsuario(usuario))
    );
  }

  actualizarPerfil(data: Pick<Usuario, 'name' | 'telefono' | 'ciudad'> & { nombreComplejo?: string }): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.usersApiUrl}/me`, data).pipe(
      tap((usuario) => this.guardarUsuario(usuario))
    );
  }

  subirAvatar(archivo: File): Observable<Usuario> {
    const formData = new FormData();
    formData.append('archivo', archivo);
    return this.http.post<Usuario>(`${this.usersApiUrl}/me/avatar`, formData).pipe(
      tap((usuario) => this.guardarUsuario(usuario))
    );
  }

  obtenerCuentaCobro(): Observable<CuentaCobro> {
    return this.http.get<CuentaCobro>(`${this.usersApiUrl}/me/cuenta-cobro`);
  }

  actualizarCuentaCobro(data: Pick<CuentaCobro, 'cbuCvu' | 'aliasMercadoPago'>): Observable<CuentaCobro> {
    return this.http.put<CuentaCobro>(`${this.usersApiUrl}/me/cuenta-cobro`, data);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario');
    this.usuarioSubject.next(null);
  }

  guardarSesion(res: any): void {
    if (res?.token) {
      localStorage.setItem('token', res.token);
    }
    if (res?.user) {
      this.guardarUsuario(res.user);
    }
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getUsuario(): Usuario | null {
    return this.usuarioSubject.value;
  }

  estaLogueado(): boolean {
    const token=this.getToken();
    if(!token)return false;
    try {const payload=JSON.parse(atob(token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/')));if(typeof payload.exp!=='number'||payload.exp*1000<=Date.now()){this.logout();return false;}return true;}
    catch {this.logout();return false;}
  }

  esDueno(): boolean {
    return this.getUsuario()?.role === 'dueno';
  }

  esAdmin(): boolean {
    return this.getUsuario()?.role === 'administrador';
  }

  private guardarUsuario(usuario: Usuario): void {
    localStorage.setItem('usuario', JSON.stringify(usuario));
    this.usuarioSubject.next(usuario);
  }

  private leerUsuarioGuardado(): Usuario | null {
    const raw = localStorage.getItem('usuario');
    if (!raw) return null;

    try {
      return JSON.parse(raw) as Usuario;
    } catch {
      localStorage.removeItem('usuario');
      return null;
    }
  }
}
