import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BitacoraService, EstadoIncidente, EventoBitacora, PaginaBitacora } from '../../services/bitacora.service';

@Component({
  selector: 'app-admin-bitacora',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-bitacora.component.html',
  styleUrl: './admin-bitacora.component.css',
})
export class AdminBitacoraComponent implements OnInit {
  readonly cargando = signal(true);
  readonly error = signal('');
  readonly pagina = signal<PaginaBitacora | null>(null);
  readonly seleccionado = signal<EventoBitacora | null>(null);
  readonly guardando = signal(false);
  readonly mensajeAccion = signal('');
  @ViewChild('detalleDialog') detalleDialog?: ElementRef<HTMLDialogElement>;
  nivel = '';
  origen = '';
  constructor(private readonly bitacora: BitacoraService) {}
  ngOnInit(): void { this.cargar(0); }
  abrir(evento: EventoBitacora): void {
    this.mensajeAccion.set('');
    this.seleccionado.set(evento);
    this.detalleDialog?.nativeElement.showModal();
  }
  cerrar(): void {
    this.detalleDialog?.nativeElement.close();
    this.seleccionado.set(null);
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

  cargar(numero: number): void {
    this.cargando.set(true);
    this.error.set('');
    this.bitacora.listar(numero, this.nivel, this.origen).subscribe({
      next: resultado => { this.pagina.set(resultado); this.cargando.set(false); },
      error: err => {
        this.error.set(err.status === 403 ? 'No tenés permisos para ver la bitácora.' :
          'No pudimos consultar la bitácora. Revisá Spring Boot y la tabla bitacora_eventos.');
        this.pagina.set(null);
        this.cargando.set(false);
      },
    });
  }
}
