import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { CanchaService } from '../../services/cancha.service';
import { DuenoPublico } from '../../models/agenda';

@Component({
  selector: 'app-canchas',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './canchas.component.html',
  styleUrl: './canchas.component.css',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class CanchasComponent implements OnInit, OnDestroy {
  complejos: DuenoPublico[] = [];
  ciudades: { id: number; nombre: string }[] = [];
  ciudadSeleccionada = '';
  tipoSeleccionado = '';
  texto = '';
  orden: 'nombre' | 'precio' = 'nombre';
  cargando = true;
  error = '';
  errorCiudades = '';
  private peticion?: Subscription;
  private ciudadesPeticion?: Subscription;

  constructor(private service: CanchaService) {}

  ngOnInit(): void {
    this.ciudadesPeticion = this.service.listarCiudades().subscribe({
      next: (resultado) => (this.ciudades = resultado),
      error: () => (this.errorCiudades = 'No se pudieron cargar las ciudades. Podés explorar todas las canchas.'),
    });
    this.buscar();
  }

  ngOnDestroy(): void {
    this.peticion?.unsubscribe();
    this.ciudadesPeticion?.unsubscribe();
  }

  buscar(): void {
    this.peticion?.unsubscribe();
    this.cargando = true;
    this.error = '';
    this.peticion = this.service.duenos({
      ciudadId: this.ciudadSeleccionada ? Number(this.ciudadSeleccionada) : undefined,
      tipo: this.tipoSeleccionado || undefined,
    }).subscribe({
      next: (resultado) => {
        this.complejos = resultado;
        this.cargando = false;
      },
      error: () => {
        this.error = 'No pudimos cargar las canchas. Revisá tu conexión.';
        this.cargando = false;
      },
    });
  }

  get visibles(): DuenoPublico[] {
    const texto = this.texto.trim().toLocaleLowerCase('es-AR');
    return this.complejos
      .filter((complejo) =>
        [complejo.nombre, ...complejo.ciudades, ...complejo.tipos]
          .filter(Boolean)
          .join(' ')
          .toLocaleLowerCase('es-AR')
          .includes(texto),
      )
      .sort((a, b) =>
        this.orden === 'precio'
          ? a.precioDesde - b.precioDesde || a.nombre.localeCompare(b.nombre, 'es-AR')
          : a.nombre.localeCompare(b.nombre, 'es-AR'),
      );
  }

  limpiarFiltros(): void {
    this.ciudadSeleccionada = '';
    this.tipoSeleccionado = '';
    this.texto = '';
    this.orden = 'nombre';
    this.buscar();
  }

  ocultarImagen(evento: Event): void {
    (evento.target as HTMLImageElement).style.display = 'none';
  }
}
