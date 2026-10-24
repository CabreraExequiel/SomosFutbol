import { ErrorHandler, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

@Injectable()
export class FrontendErrorHandler implements ErrorHandler {
  private ultimo = 0;
  constructor(private readonly http: HttpClient, private readonly auth: AuthService) {}

  handleError(error: unknown): void {
    // Los errores siguen visibles en la consola local para depuración detallada.
    console.error(error);
    if (!this.auth.estaLogueado()) return;
    const ahora = Date.now();
    if (ahora - this.ultimo < 15_000) return;
    this.ultimo = ahora;
    const valor = error instanceof Error ? error : null;
    const nombre = valor?.name || 'Error';
    // Solo informar un nombre de función corto y seguro si el stack permite aislarlo.
    // Nunca enviar el stack, ruta con query, mensaje, datos de usuario o encabezados.
    const funcion = this.funcionDeAplicacion(valor?.stack);
    this.http.post(`${environment.apiUrl}/diagnostico/frontend`, {
      nombre: nombre.slice(0, 70),
      ruta: window.location.pathname.slice(0, 180),
      funcion,
    }).subscribe({ error: () => { /* registro remoto es opcional */ } });
  }

  private funcionDeAplicacion(stack?: string): string | null {
    if (!stack) return null;
    // Navegadores en desarrollo muestran rutas src/app; en producción minificada
    // no podemos inferir una función fiable y preferimos null.
    for (const linea of stack.split('\n').slice(1, 15)) {
      if (!/(?:src\/app\/|\.component\.ts|\.service\.ts)/.test(linea)) continue;
      const coincidencia = linea.match(/^\s*at\s+([A-Za-z_$][A-Za-z0-9_$.]{0,119})\s*\(/);
      if (coincidencia) return coincidencia[1];
    }
    return null;
  }
}
