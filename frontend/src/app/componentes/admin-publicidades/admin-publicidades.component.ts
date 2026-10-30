import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { PublicidadesEstadisticasComponent } from '../publicidades-estadisticas/publicidades-estadisticas.component';
import { HttpErrorResponse } from '@angular/common/http';
import {
  EstadoPublicidad, UbicacionPublicidad, PublicidadAdmin,
  PublicidadSolicitud, PaginaPublicidades, PublicidadesAdminService
} from '../../services/publicidades-admin.service';

interface FormularioPublicidad {
  titulo: string;
  descripcion: string;
  imagenUrl: string;
  destinoUrl: string;
  ubicacion: UbicacionPublicidad;
  prioridad: number;
  fechaInicio: string;
  fechaFin: string;
}

function formularioVacio(): FormularioPublicidad {
  return {
    titulo: '', descripcion: '', imagenUrl: '', destinoUrl: '',
    ubicacion: 'FEED', prioridad: 0, fechaInicio: '', fechaFin: ''
  };
}

/** Etapa 10B: CRUD de publicidades, solo dentro del panel protegido /admin. */
@Component({
  selector: 'app-admin-publicidades',
  standalone: true,
  imports: [CommonModule, FormsModule, PublicidadesEstadisticasComponent],
  templateUrl: './admin-publicidades.component.html',
  styleUrl: './admin-publicidades.component.css',
})
export class AdminPublicidadesComponent implements OnInit {
  readonly pagina = signal<PaginaPublicidades | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly exito = signal('');
  readonly guardando = signal(false);
  readonly cambiandoId = signal<number | null>(null);
  readonly editandoId = signal<number | null>(null);
  readonly formularioAbierto = signal(false);
  readonly cargandoDetalle = signal(false);
  readonly estados: EstadoPublicidad[] = ['BORRADOR', 'ACTIVA', 'PAUSADA', 'ARCHIVADA'];
  readonly ubicaciones: UbicacionPublicidad[] = ['INICIO', 'FEED', 'BUSCAR_CANCHAS'];

  buscar = '';
  estado = '';
  ubicacion = '';
  formulario: FormularioPublicidad = formularioVacio();
  private solicitudActual = 0;

  constructor(private readonly servicio: PublicidadesAdminService) {}

  ngOnInit(): void { this.cargar(); }

  cargar(numero = 0): void {
    const id = ++this.solicitudActual;
    this.cargando.set(true);
    this.error.set('');
    this.servicio.listar(this.buscar.trim(), this.estado, this.ubicacion, numero).subscribe({
      next: datos => {
        if (id !== this.solicitudActual) return;
        this.pagina.set(datos);
        this.cargando.set(false);
      },
      error: e => {
        if (id !== this.solicitudActual) return;
        this.error.set(this.mensajeError(e, 'No se pudo consultar la lista.'));
        this.cargando.set(false);
      }
    });
  }

  buscarAnuncios(): void { this.exito.set(''); this.cargar(0); }

  limpiarFiltros(): void {
    this.buscar = '';
    this.estado = '';
    this.ubicacion = '';
    this.buscarAnuncios();
  }

  nueva(): void {
    this.formulario = formularioVacio();
    this.editandoId.set(null);
    this.error.set(''); this.exito.set('');
    this.formularioAbierto.set(true);
  }

  editar(anuncio: PublicidadAdmin): void {
    if (anuncio.estado === 'ARCHIVADA' || this.cargandoDetalle()) return;
    this.cargandoDetalle.set(true);
    this.error.set(''); this.exito.set('');
    this.servicio.detalle(anuncio.id).subscribe({
      next: actual => {
        this.formulario = {
          titulo: actual.titulo,
          descripcion: actual.descripcion ?? '',
          imagenUrl: actual.imagenUrl ?? '',
          destinoUrl: actual.destinoUrl,
          ubicacion: actual.ubicacion,
          prioridad: actual.prioridad,
          fechaInicio: this.aLocal(actual.fechaInicio),
          fechaFin: this.aLocal(actual.fechaFin)
        };
        this.editandoId.set(actual.id);
        this.formularioAbierto.set(true);
        this.cargandoDetalle.set(false);
      },
      error: e => {
        this.error.set(this.mensajeError(e, 'No se pudo abrir la publicidad.'));
        this.cargandoDetalle.set(false);
      }
    });
  }

  cancelar(): void {
    if (this.guardando()) return;
    this.formularioAbierto.set(false);
    this.editandoId.set(null);
    this.formulario = formularioVacio();
  }

  guardar(): void {
    if (this.guardando()) return;
    const error = this.validarFormulario();
    if (error) { this.error.set(error); return; }
    const solicitud: PublicidadSolicitud = {
      titulo: this.formulario.titulo.trim(),
      descripcion: this.formulario.descripcion.trim() || null,
      imagenUrl: this.formulario.imagenUrl.trim() || null,
      destinoUrl: this.formulario.destinoUrl.trim(),
      ubicacion: this.formulario.ubicacion,
      prioridad: Number(this.formulario.prioridad),
      fechaInicio: this.aIso(this.formulario.fechaInicio),
      fechaFin: this.aIso(this.formulario.fechaFin),
    };
    const id = this.editandoId();
    this.guardando.set(true);
    this.error.set(''); this.exito.set('');
    const resultado = id === null ? this.servicio.crear(solicitud) : this.servicio.editar(id, solicitud);
    resultado.subscribe({
      next: anuncio => {
        this.guardando.set(false);
        this.formularioAbierto.set(false);
        this.editandoId.set(null);
        this.exito.set(id === null ? `Publicidad #${anuncio.id} creada en BORRADOR.` : `Publicidad #${anuncio.id} actualizada.`);
        this.cargar(0);
      },
      error: e => {
        this.error.set(this.mensajeError(e, 'No se pudo guardar la publicidad.'));
        this.guardando.set(false);
      }
    });
  }

  cambiarEstado(anuncio: PublicidadAdmin, nuevo: EstadoPublicidad): void {
    if (anuncio.estado === 'ARCHIVADA' || this.cambiandoId() !== null || this.guardando()) return;
    const confirmacion = nuevo === 'ARCHIVADA'
      ? `¿Archivar definitivamente «${anuncio.titulo}»? La publicidad archivada NO puede editarse ni reactivarse.`
      : `¿Cambiar «${anuncio.titulo}» a ${nuevo}?`;
    if (!window.confirm(confirmacion)) return;
    this.cambiandoId.set(anuncio.id);
    this.error.set(''); this.exito.set('');
    this.servicio.cambiarEstado(anuncio.id, nuevo).subscribe({
      next: actualizado => {
        this.cambiandoId.set(null);
        this.exito.set(`Publicidad #${actualizado.id}: ${actualizado.estado}.`);
        this.cargar(this.pagina()?.number ?? 0);
      },
      error: e => {
        this.cambiandoId.set(null);
        this.error.set(this.mensajeError(e, 'No se pudo cambiar el estado.'));
      }
    });
  }

  ubicacionEtiqueta(valor: UbicacionPublicidad): string {
    switch (valor) {
      case 'INICIO': return 'Inicio';
      case 'FEED': return 'Feed';
      case 'BUSCAR_CANCHAS': return 'Buscar canchas';
    }
  }

  abrirDestino(url: string): void {
    // Nunca abrir URLs sin https (defensa adicional al validador de Spring Boot).
    if (!this.urlValida(url)) return;
    window.open(url, '_blank', 'noopener,noreferrer');
  }

  private validarFormulario(): string {
    const f = this.formulario;
    if (!f.titulo.trim() || f.titulo.trim().length > 120) return 'El título debe tener entre 1 y 120 caracteres.';
    if (f.descripcion.trim().length > 400) return 'La descripción puede tener hasta 400 caracteres.';
    if (!this.urlValida(f.destinoUrl.trim())) return 'Ingresá un enlace de destino HTTPS válido.';
    if (f.imagenUrl.trim() && !this.urlValida(f.imagenUrl.trim())) return 'La imagen debe tener una URL HTTPS válida.';
    const prioridad = Number(f.prioridad);
    if (!Number.isInteger(prioridad) || prioridad < 0 || prioridad > 100) return 'La prioridad debe ser un entero entre 0 y 100.';
    const inicio = f.fechaInicio ? new Date(f.fechaInicio) : null;
    const fin = f.fechaFin ? new Date(f.fechaFin) : null;
    if ((inicio && Number.isNaN(inicio.getTime())) || (fin && Number.isNaN(fin.getTime()))) return 'Revisá las fechas de publicación.';
    if (inicio && fin && fin <= inicio) return 'La fecha final debe ser posterior a la inicial.';
    return '';
  }

  private urlValida(url: string): boolean {
    try {
      const parsed = new URL(url);
      return parsed.protocol === 'https:' && !!parsed.hostname && !parsed.username && !parsed.password
        && (!parsed.port || parsed.port === '443') && url.length <= 700;
    } catch { return false; }
  }

  private aIso(valor: string): string | null { return valor ? new Date(valor).toISOString() : null; }

  private aLocal(valor: string | null): string {
    if (!valor) return '';
    const fecha = new Date(valor);
    if (Number.isNaN(fecha.getTime())) return '';
    const d = (n: number) => n.toString().padStart(2, '0');
    return `${fecha.getFullYear()}-${d(fecha.getMonth() + 1)}-${d(fecha.getDate())}T${d(fecha.getHours())}:${d(fecha.getMinutes())}`;
  }

  private mensajeError(error: HttpErrorResponse, fallback: string): string {
    if (error.status === 401) return 'Tu sesión venció. Volvé a iniciar sesión.';
    if (error.status === 403) return 'Tu cuenta no tiene permisos de administrador.';
    if (error.status === 404) return 'La publicidad ya no existe.';
    if (error.status === 409) return 'La publicidad fue archivada o no permite esa operación. Actualizá la lista.';
    const servidor = error.error;
    if (error.status === 400 && typeof servidor?.message === 'string') return servidor.message;
    if (error.status === 0) return 'No hay conexión con Spring Boot. Comprobá localhost:8080.';
    return fallback;
  }
}
