import { Component, Input, OnDestroy, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { PublicidadImpresionDirective } from '../../directivas/publicidad-impresion.directive';
import { PublicidadesMetricasService } from '../../services/publicidades-metricas.service';
import {
  PublicidadVisible,
  PublicidadesPublicasService,
  UbicacionPublicidadPublica
} from '../../services/publicidades-publicas.service';

/** Espacio discreto de anuncios; si no hay anuncios vigentes no ocupa espacio. */
@Component({
  selector: 'app-publicidades-visibles',
  standalone: true,
  imports: [CommonModule, PublicidadImpresionDirective],
  templateUrl: './publicidades-visibles.component.html',
  styleUrl: './publicidades-visibles.component.css'
})
export class PublicidadesVisiblesComponent implements OnInit, OnDestroy {
  @Input({ required: true }) ubicacion!: UbicacionPublicidadPublica;
  readonly anuncios = signal<PublicidadVisible[]>([]);
  private peticion?: Subscription;

  constructor(private readonly servicio: PublicidadesPublicasService,
              private readonly metricas: PublicidadesMetricasService) {}

  ngOnInit(): void {
    this.peticion = this.servicio.listar(this.ubicacion).subscribe({
      next: (datos) => {
        this.anuncios.set(Array.isArray(datos)
          ? datos.filter(a => this.enlaceHttpsSeguro(a.destinoUrl))
          : []);
      },
      error: () => { this.anuncios.set([]); } // Publicidades opcionales: nunca bloquean el feed/canchas.
    });
  }

  ngOnDestroy(): void {
    this.peticion?.unsubscribe();
  }

  registrarClick(anuncio: PublicidadVisible): void {
    this.metricas.clic(anuncio.id, this.ubicacion).subscribe({ error: () => {} });
  }

  imagenSegura(url: string | null): boolean {
    return !!url && this.enlaceHttpsSeguro(url);
  }

  private enlaceHttpsSeguro(valor: string | null | undefined): boolean {
    if (!valor) return false;
    try {
      const u = new URL(valor);
      return u.protocol === 'https:' && !!u.hostname && !u.username && !u.password;
    } catch {
      return false;
    }
  }
}
