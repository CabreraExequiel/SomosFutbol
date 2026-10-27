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
  styleUrls: ['./canchas.component.css', './canchas-filtros-06c.css'],
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class CanchasComponent implements OnInit, OnDestroy {
  complejos: DuenoPublico[] = [];
  ciudades: { id: number; nombre: string }[] = [];
  ciudadSeleccionada = '';
  tipoSeleccionado = '';
  texto = '';
  orden: 'nombre' | 'precio' | 'cantidad' = 'nombre';

  // Los importes filtran el campo precioDesde que devuelve /canchas/duenos.
  // No se presenta como un filtro del precio de cada cancha individual.
  precioDesdeMinimo: number | null = null;
  precioDesdeMaximo: number | null = null;
  minimoCanchas: number | null = null;

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

  get rangoPrecioInvalido(): boolean {
    const minimo = this.importe(this.precioDesdeMinimo);
    const maximo = this.importe(this.precioDesdeMaximo);
    return (minimo !== null && minimo < 0)
      || (maximo !== null && maximo < 0)
      || (minimo !== null && maximo !== null && minimo > maximo);
  }

  get minimoCanchasInvalido(): boolean {
    const cantidad = this.importe(this.minimoCanchas);
    return cantidad !== null && (!Number.isInteger(cantidad) || cantidad < 1);
  }

  get filtrosActivos(): boolean {
    return !!(
      this.texto.trim() || this.ciudadSeleccionada || this.tipoSeleccionado
      || this.precioDesdeMinimo !== null || this.precioDesdeMaximo !== null
      || this.minimoCanchas !== null || this.orden !== 'nombre'
    );
  }

  get visibles(): DuenoPublico[] {
    if (this.rangoPrecioInvalido || this.minimoCanchasInvalido) return [];

    const texto = this.normalizar(this.texto.trim());
    const minimo = this.importe(this.precioDesdeMinimo);
    const maximo = this.importe(this.precioDesdeMaximo);
    const cantidadMinima = this.importe(this.minimoCanchas);

    return this.complejos
      .filter((complejo) => {
        const campos = [complejo.nombre, ...complejo.ciudades, ...complejo.tipos];
        if (texto && !this.normalizar(campos.filter(Boolean).join(' ')).includes(texto)) return false;
        if (minimo !== null && complejo.precioDesde < minimo) return false;
        if (maximo !== null && complejo.precioDesde > maximo) return false;
        if (cantidadMinima !== null && complejo.cantidadCanchas < cantidadMinima) return false;
        return true;
      })
      .sort((a, b) => {
        const nombre = a.nombre.localeCompare(b.nombre, 'es-AR', { sensitivity: 'base' });
        if (this.orden === 'precio') return a.precioDesde - b.precioDesde || nombre;
        if (this.orden === 'cantidad') return b.cantidadCanchas - a.cantidadCanchas || nombre;
        return nombre;
      });
  }

  limpiarFiltros(): void {
    const filtraEnServidor = !!(this.ciudadSeleccionada || this.tipoSeleccionado);
    this.ciudadSeleccionada = '';
    this.tipoSeleccionado = '';
    this.texto = '';
    this.orden = 'nombre';
    this.precioDesdeMinimo = null;
    this.precioDesdeMaximo = null;
    this.minimoCanchas = null;
    // Si la consulta anterior estaba acotada por ciudad/formato, recuperar el listado completo.
    // Caso contrario, basta restablecer filtros locales sin solicitar datos otra vez.
    if (filtraEnServidor) this.buscar();
  }

  ocultarImagen(evento: Event): void {
    (evento.target as HTMLImageElement).style.display = 'none';
  }

  private importe(valor: number | null): number | null {
    if (valor === null || valor === undefined) return null;
    const numero = Number(valor);
    return Number.isFinite(numero) ? numero : null;
  }

  private normalizar(valor: string): string {
    return valor.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es-AR');
  }
}
