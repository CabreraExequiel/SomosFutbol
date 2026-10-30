import { CommonModule } from '@angular/common';
import { Component, computed, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EstadisticasPublicidad, PublicidadesMetricasService } from '../../services/publicidades-metricas.service';

@Component({
  selector: 'app-publicidades-estadisticas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './publicidades-estadisticas.component.html',
  styleUrl: './publicidades-estadisticas.component.css'
})
export class PublicidadesEstadisticasComponent implements OnInit {
  readonly datos = signal<EstadisticasPublicidad | null>(null);
  readonly cargando = signal(false);
  readonly error = signal('');
  readonly maxImpresiones = computed(() => Math.max(1, ...(this.datos()?.evolucion ?? []).map(d => d.impresiones)));
  dias: 7 | 30 = 7;
  ubicacion = '';
  publicidadId = '';
  private version = 0;

  constructor(private readonly servicio: PublicidadesMetricasService) {}
  ngOnInit(): void { this.cargar(); }

  cargar(): void {
    const id = ++this.version;
    const texto = this.publicidadId.trim();
    if (texto && (!/^\d+$/.test(texto) || Number(texto) < 1 || !Number.isSafeInteger(Number(texto)))) {
      this.error.set('Ingresá un ID de publicidad válido.');
      return;
    }
    this.cargando.set(true); this.error.set('');
    this.servicio.estadisticas(this.dias, this.ubicacion, texto).subscribe({
      next: datos => {
        if (id !== this.version) return;
        this.datos.set(datos); this.cargando.set(false);
      },
      error: err => {
        if (id !== this.version) return;
        this.cargando.set(false);
        this.error.set(err.status === 403 ? 'No tenés permisos de administrador.' :
          err.status === 401 ? 'La sesión venció. Volvé a ingresar.' :
          'No se pudieron obtener las estadísticas. Revisá el backend y la migración V19.');
      }
    });
  }

  limpiar(): void {
    this.dias = 7; this.ubicacion = ''; this.publicidadId = ''; this.cargar();
  }

  altura(valor: number): number {
    return valor <= 0 ? 2 : Math.max(4, Math.round((valor / this.maxImpresiones()) * 110));
  }

  etiqueta(fecha: string): string { return fecha.slice(5); }
}
