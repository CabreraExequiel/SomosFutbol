import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BitacoraService, EstadisticasBitacora, EstadoIncidente, EventoBitacora, PaginaBitacora } from '../../services/bitacora.service';

@Component({
  selector: 'app-admin-bitacora',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-bitacora.component.html',
  styleUrl: './admin-bitacora.component.css',
})
export class AdminBitacoraComponent implements OnInit {
  // 09E4: estadísticas propias; no dependen de la paginación del listado principal.
  readonly estadisticasCargando = signal(false);
  readonly estadisticasError = signal('');
  readonly estadisticasDatos = signal<EstadisticasBitacora | null>(null);
  diasEstadisticas: 7 | 30 = 7;
  ambienteEstadisticas = '';
  private ultimaSolicitudEstadisticas = 0;
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly pagina = signal<PaginaBitacora | null>(null);
  readonly seleccionado = signal<EventoBitacora | null>(null);
  readonly guardando = signal(false);
  readonly mensajeAccion = signal('');
  readonly incidentesCargando = signal(false);
  readonly incidentesError = signal('');
  readonly incidentesPagina = signal<PaginaBitacora | null>(null);
  readonly historialIncidentePagina = signal<PaginaBitacora | null>(null);
  readonly historialIncidenteCargando = signal(false);
  readonly historialIncidenteError = signal('');
  estadoSeguimiento = 'PENDIENTE';
  origenSeguimiento = '';
  ambienteSeguimiento = '';
  ultimoAdminSeguimiento = '';
  sinGestionSeguimiento = false;
  buscarSeguimiento = '';
  private ultimaSolicitudIncidentes = 0;
  private ultimaSolicitudHistorial = 0;
  readonly accionesCargando = signal(false);
  readonly accionesError = signal('');
  readonly accionesPagina = signal<PaginaBitacora | null>(null);
  tipoAccion = '';
  actorAccion = '';
  buscarAccion = '';
  private ultimaSolicitudAcciones = 0;
  @ViewChild('detalleDialog') detalleDialog?: ElementRef<HTMLDialogElement>;
  nivel = '';
  origen = '';
  ambiente = '';
  estado = '';
  buscar = '';
  private ultimaSolicitud = 0;
  constructor(private readonly bitacora: BitacoraService) {}

  cargarEstadisticas(): void {
    const solicitud = ++this.ultimaSolicitudEstadisticas;
    this.estadisticasCargando.set(true);
    this.estadisticasError.set('');
    this.bitacora.estadisticas(this.diasEstadisticas, this.ambienteEstadisticas).subscribe({
      next: datos => {
        if (solicitud !== this.ultimaSolicitudEstadisticas) return;
        this.estadisticasDatos.set(datos);
        this.estadisticasCargando.set(false);
      },
      error: err => {
        if (solicitud !== this.ultimaSolicitudEstadisticas) return;
        this.estadisticasDatos.set(null);
        this.estadisticasCargando.set(false);
        this.estadisticasError.set(err.status === 403 ? 'Acceso denegado a las estadísticas.' :
          err.status === 400 ? 'Período o entorno inválido.' :
          'No se pudieron consultar las estadísticas. Revisá Spring Boot.');
      },
    });
  }

  alturaBarra(errores: number, maximo: number): number {
    return maximo > 0 && errores > 0 ? Math.max(5, Math.round(100 * errores / maximo)) : 0;
  }

  maximoTendencia(datos: EstadisticasBitacora): number {
    return Math.max(0, ...datos.tendencia.map(d => d.errores));
  }

  limpiarFiltros(): void {
    this.nivel = '';
    this.origen = '';
    this.ambiente = '';
    this.estado = '';
    this.buscar = '';
    this.cargar(0);
  }
  ngOnInit(): void { this.cargar(0); this.cargarAcciones(0); this.cargarIncidentes(0); this.cargarEstadisticas(); }
  abrir(evento: EventoBitacora): void {
    this.mensajeAccion.set('');
    this.seleccionado.set(evento);
    this.historialIncidentePagina.set(null);
    this.historialIncidenteError.set('');
    this.detalleDialog?.nativeElement.showModal();
    if (evento.nivel === 'ERROR') this.cargarHistorialIncidente(evento.id, 0);
  }
  cerrar(): void {
    ++this.ultimaSolicitudHistorial;
    this.detalleDialog?.nativeElement.close();
    this.seleccionado.set(null);
    this.historialIncidentePagina.set(null);
  }
  cambiarEstado(estado: EstadoIncidente): void {
    const evento = this.seleccionado();
    if (!evento || evento.nivel !== 'ERROR' || this.guardando()) return;
    this.guardando.set(true);
    this.mensajeAccion.set('');
    this.bitacora.actualizarEstado(evento.id, estado).subscribe({
      next: actualizado => {
        this.seleccionado.set(actualizado);
        this.pagina.update(pagina => pagina ? {
          ...pagina, content: pagina.content.map(e => e.id === actualizado.id ? actualizado : e),
        } : null);
        this.guardando.set(false);
        this.mensajeAccion.set('Estado actualizado correctamente.');
        this.cargarAcciones(0);
        this.cargarIncidentes(this.incidentesPagina()?.number ?? 0);
        this.cargarHistorialIncidente(actualizado.id, 0);
        this.cargarEstadisticas();
      },
      error: () => {
        this.guardando.set(false);
        this.mensajeAccion.set('No se pudo actualizar el estado. Intentá nuevamente.');
      },
    });
  }

  etiquetaEstado(estado: EstadoIncidente | null | undefined): string {
    switch (estado) {
      case 'EN_INVESTIGACION': return 'En investigación';
      case 'RESUELTO': return 'Resuelto';
      default: return 'Pendiente';
    }
  }

  async copiarDiagnostico(): Promise<void> {
    const e = this.seleccionado();
    if (!e) return;
    const diagnostico = [
      `Somos Fútbol · ${e.codigo}`,
      `Fecha: ${e.fecha} | Entorno: ${e.ambiente}`,
      `Nivel: ${e.nivel} | Origen: ${e.origen} | Tipo: ${e.tipo}`,
      `Descripción: ${e.descripcion}`,
      `Estado: ${e.estado}`,
      `Ruta: ${e.ruta || 'No disponible'}`,
      `Clase: ${e.claseOrigen || 'No identificada'}`,
      `Función: ${e.funcionOrigen || 'No identificada'}`,
      `Archivo: ${e.archivoOrigen || 'No disponible'} | Línea: ${e.lineaOrigen ?? 'No disponible'}`,
      `SQLState: ${e.sqlState || '—'} | Código SQL: ${e.codigoSql ?? '—'}`,
      `Diagnóstico: ${e.causaProbable || 'Sin diagnóstico'}`,
    ].join('\n');
    try {
      await navigator.clipboard.writeText(diagnostico);
      this.mensajeAccion.set('Diagnóstico copiado. Revisá antes de compartirlo externamente.');
    } catch {
      this.mensajeAccion.set('El navegador no permitió copiar. Usá un contexto seguro (localhost o HTTPS).');
    }
  }

  limpiarSeguimiento(): void {
    this.estadoSeguimiento = 'PENDIENTE';
    this.origenSeguimiento = '';
    this.ambienteSeguimiento = '';
    this.ultimoAdminSeguimiento = '';
    this.sinGestionSeguimiento = false;
    this.buscarSeguimiento = '';
    this.cargarIncidentes(0);
  }

  cargarIncidentes(numero: number): void {
    const solicitud = ++this.ultimaSolicitudIncidentes;
    this.incidentesCargando.set(true);
    this.incidentesError.set('');
    const texto = this.ultimoAdminSeguimiento.trim();
    const ultimoAdminId = /^\d+$/.test(texto) ? Number(texto) : undefined;
    if (!this.sinGestionSeguimiento && texto && (!ultimoAdminId || !Number.isSafeInteger(ultimoAdminId))) {
      this.incidentesCargando.set(false);
      this.incidentesError.set('Ingresá un ID de administrador válido.');
      return;
    }
    this.bitacora.listarIncidentes(numero, {
      estado: this.estadoSeguimiento,
      origen: this.origenSeguimiento,
      ambiente: this.ambienteSeguimiento,
      ultimoAdminId: this.sinGestionSeguimiento ? undefined : ultimoAdminId,
      sinGestion: this.sinGestionSeguimiento,
      buscar: this.buscarSeguimiento,
    }).subscribe({
      next: datos => {
        if (solicitud !== this.ultimaSolicitudIncidentes) return;
        this.incidentesPagina.set(datos);
        this.incidentesCargando.set(false);
      },
      error: err => {
        if (solicitud !== this.ultimaSolicitudIncidentes) return;
        this.incidentesError.set(err.status === 403 ? 'No tenés permiso para consultar incidentes.' :
          err.status === 400 ? 'Revisá los filtros del seguimiento.' :
          'No pudimos consultar los incidentes. Revisá Spring Boot.');
        this.incidentesPagina.set(null);
        this.incidentesCargando.set(false);
      },
    });
  }

  cargarHistorialIncidente(id: number, numero: number): void {
    if (this.seleccionado()?.id !== id || this.seleccionado()?.nivel !== 'ERROR') return;
    const solicitud = ++this.ultimaSolicitudHistorial;
    this.historialIncidenteCargando.set(true);
    this.historialIncidenteError.set('');
    this.bitacora.historialIncidente(id, numero).subscribe({
      next: datos => {
        if (solicitud !== this.ultimaSolicitudHistorial || this.seleccionado()?.id !== id) return;
        this.historialIncidentePagina.set(datos);
        this.historialIncidenteCargando.set(false);
      },
      error: err => {
        if (solicitud !== this.ultimaSolicitudHistorial || this.seleccionado()?.id !== id) return;
        this.historialIncidenteError.set(err.status === 403 ? 'Acceso denegado al historial.' :
          'No pudimos consultar el historial de este incidente.');
        this.historialIncidenteCargando.set(false);
      },
    });
  }

  limpiarAcciones(): void {
    this.tipoAccion = '';
    this.actorAccion = '';
    this.buscarAccion = '';
    this.cargarAcciones(0);
  }

  cargarAcciones(numero: number): void {
    const solicitud = ++this.ultimaSolicitudAcciones;
    this.accionesCargando.set(true);
    this.accionesError.set('');
    const valorActor = this.actorAccion.trim();
    const administradorId = /^\d+$/.test(valorActor) ? Number(valorActor) : undefined;
    if (valorActor && (!administradorId || !Number.isSafeInteger(administradorId))) {
      this.accionesError.set('Ingresá un ID de administrador válido.');
      this.accionesCargando.set(false);
      return;
    }
    this.bitacora.listarAcciones(numero, {
      tipo: this.tipoAccion,
      administradorId,
      buscar: this.buscarAccion,
    }).subscribe({
      next: resultado => {
        if (solicitud !== this.ultimaSolicitudAcciones) return;
        this.accionesPagina.set(resultado);
        this.accionesCargando.set(false);
      },
      error: err => {
        if (solicitud !== this.ultimaSolicitudAcciones) return;
        this.accionesError.set(err.status === 403 ? 'No tenés permiso para consultar las acciones.' :
          err.status === 400 ? 'Los filtros del historial no son válidos.' :
          'No pudimos cargar las acciones administrativas.');
        this.accionesPagina.set(null);
        this.accionesCargando.set(false);
      },
    });
  }

  cargar(numero: number): void {
    const solicitud = ++this.ultimaSolicitud;
    this.cargando.set(true);
    this.error.set('');
    this.bitacora.listar(numero, {
      nivel: this.nivel,
      origen: this.origen,
      ambiente: this.ambiente,
      estado: this.estado,
      buscar: this.buscar,
    }).subscribe({
      next: resultado => {
        if (solicitud !== this.ultimaSolicitud) return;
        this.pagina.set(resultado);
        this.cargando.set(false);
      },
      error: err => {
        if (solicitud !== this.ultimaSolicitud) return;
        this.error.set(err.status === 403 ? 'No tenés permisos para ver la bitácora.' :
          err.status === 400 ? 'Alguno de los filtros no es válido.' :
          'No pudimos consultar la bitácora. Revisá Spring Boot y la tabla bitacora_eventos.');
        this.pagina.set(null);
        this.cargando.set(false);
      },
    });
  }
}
