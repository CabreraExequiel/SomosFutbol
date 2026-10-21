import { CommonModule } from '@angular/common';
import { Component, ElementRef, OnInit, ViewChild, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { BitacoraService, EventoBitacora, PaginaBitacora } from '../../services/bitacora.service';

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
  @ViewChild('detalleDialog') detalleDialog?: ElementRef<HTMLDialogElement>;
  nivel = '';
  origen = '';
  constructor(private readonly bitacora: BitacoraService) {}
  ngOnInit(): void { this.cargar(0); }
  abrir(evento: EventoBitacora): void {
    this.seleccionado.set(evento);
    this.detalleDialog?.nativeElement.showModal();
  }
  cerrar(): void {
    this.detalleDialog?.nativeElement.close();
    this.seleccionado.set(null);
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


