import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CanchaService } from '../../services/cancha.service';
import { ReservaService } from '../../services/reserva.service';
import { AgendaCanchaComponent } from '../agenda-cancha/agenda-cancha.component';
import { ReservaDetalleComponent } from '../reserva-detalle/reserva-detalle.component';
import { catchError, map, of, switchMap } from 'rxjs';

@Component({
    selector: 'app-dashboard-dueno',
    imports: [CommonModule, FormsModule, RouterLink, AgendaCanchaComponent, ReservaDetalleComponent],
    templateUrl: './dashboard-dueno.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './dashboard-dueno.component.css'
})
export class DashboardDuenoComponent implements OnInit {
  agendaCancha:any=null;
  reservaSeleccionada:number|null=null;
  editandoId:number|null=null;
  filtroEstado=''; filtroFecha=''; filtroCancha='';
  mensajeExito='';
  reservaVencida(r:any):boolean{return r.estado==='PENDIENTE'&&r.expiresAt&&Date.parse(r.expiresAt)<=Date.now();}
  get reservasFiltradas():any[]{return this.reservas.filter(r=>(!this.filtroEstado||r.estado===this.filtroEstado)&&(!this.filtroFecha||r.fecha===this.filtroFecha)&&(!this.filtroCancha||r.canchaId===Number(this.filtroCancha)));}
  editar(c:any):void {this.editandoId=c.id;this.nuevaCancha={...c};this.mostrarFormulario=true;this.errorFormulario='';window.scrollTo({top:0,behavior:'smooth'});}
  cambiarPublicacion(c:any):void {this.eliminandoId=c.id;this.canchaService.publicar(c.id,!c.activa).subscribe({next:r=>{this.canchas=this.canchas.map(x=>x.id===r.id?r:x);this.eliminandoId=null;this.mensajeExito=r.activa?'Cancha publicada.':'Cancha pausada. Sus reservas se conservan.';},error:e=>{this.eliminandoId=null;this.errorCanchas=e.error?.message||'No se pudo cambiar la publicación.';}});}
  canchas: any[] = [];
  reservas: any[] = [];
  ciudades: any[] = [];

  nuevaCancha: any = {
    nombre: '',
    ciudadId: '',
    direccion: '',
    tipo: 'futbol 5',
    superficie: 'sintetico',
    cantJugadores: 10,
    precioHora: 0,
    techada: false,
    descripcion: '',
  };

  mostrarFormulario = false;
  cargandoCanchas = true;
  cargandoReservas = true;
  guardando = false;
  imagenSeleccionada: File | null = null;
  imagenPreview = '';
  subiendoImagenId: number | null = null;
  eliminandoId: number | null = null;
  procesandoReservaId: number | null = null;
  errorCanchas = '';
  errorReservas = '';
  errorFormulario = '';

  constructor(private canchaService: CanchaService, private reservaService: ReservaService) {}

  ngOnInit(): void {
    this.cargarCanchas();
    this.cargarReservas();
    this.canchaService.listarCiudades().subscribe({next:(res: any) => this.ciudades = res,error:()=>this.errorFormulario='No se pudo cargar el listado de ciudades. Recargá la página.'});
  }

  cargarCanchas(): void {
    this.cargandoCanchas = true;
    this.errorCanchas = '';
    this.canchaService.misCanchas().subscribe({
      next: (res: any) => {
        this.canchas = res.data ?? res;
        this.cargandoCanchas = false;
      },
      error: () => {
        this.errorCanchas = 'Revisá tu conexión e intentá nuevamente.';
        this.cargandoCanchas = false;
      },
    });
  }

  cargarReservas(): void {
    this.cargandoReservas = true;
    this.errorReservas = '';
    this.reservaService.reservasDeMisCanchas().subscribe({
      next: (res: any) => {
        this.reservas = res.data ?? res;
        this.cargandoReservas = false;
      },
      error: () => {
        this.errorReservas = 'Revisá tu conexión e intentá nuevamente.';
        this.cargandoReservas = false;
      },
    });
  }

  crearCancha(): void {
    if(this.guardando)return;
    this.guardando = true;
    this.errorFormulario = '';
    const imagen = this.imagenSeleccionada;
    const operacion=this.editandoId?this.canchaService.actualizar(this.editandoId,this.nuevaCancha):this.canchaService.crear(this.nuevaCancha);
    operacion.pipe(
      switchMap((cancha: any) => {
        if (!imagen) return of({ cancha, errorImagen: false });
        return this.canchaService.subirImagen(cancha.id, imagen).pipe(
          map((actualizada) => ({ cancha: actualizada, errorImagen: false })),
          catchError(() => of({ cancha, errorImagen: true }))
        );
      })
    ).subscribe({
      next: ({ cancha, errorImagen }) => {
        this.mensajeExito='Cancha guardada. Configurá los días y horarios para recibir reservas.';
        this.agendaCancha=cancha;
        this.editandoId=null;
        this.guardando = false;
        this.mostrarFormulario = false;
        this.reiniciarFormulario();
        if (errorImagen) {
          this.errorCanchas = 'La cancha se publicó, pero no pudimos subir la foto. Podés agregarla desde la tarjeta.';
        }
        this.cargarCanchas();
      },
      error: (err) => {
        this.guardando = false;
        this.errorFormulario = err?.error?.message || Object.values(err?.error||{}).join(' ') || 'No se pudo guardar la cancha. Revisá los datos.';
      },
    });
  }

  seleccionarImagen(event: Event): void {
    this.errorFormulario = '';
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;

    if (!['image/jpeg', 'image/png'].includes(archivo.type)) {
      this.errorFormulario = 'La foto debe estar en formato JPG o PNG.';
      input.value = '';
      return;
    }
    if (archivo.size > 5 * 1024 * 1024) {
      this.errorFormulario = 'La foto no puede superar los 5 MB.';
      input.value = '';
      return;
    }

    this.imagenSeleccionada = archivo;
    const reader = new FileReader();
    reader.onload = () => (this.imagenPreview = String(reader.result || ''));
    reader.readAsDataURL(archivo);
  }

  cambiarImagen(cancha: any, event: Event): void {
    const input = event.target as HTMLInputElement;
    const archivo = input.files?.[0];
    if (!archivo) return;
    if (!['image/jpeg', 'image/png'].includes(archivo.type) || archivo.size > 5 * 1024 * 1024) {
      this.errorCanchas = 'Elegí una imagen JPG o PNG de hasta 5 MB.';
      input.value = '';
      return;
    }

    this.subiendoImagenId = cancha.id;
    this.errorCanchas = '';
    this.canchaService.subirImagen(cancha.id, archivo).subscribe({
      next: (actualizada: any) => {
        this.canchas = this.canchas.map((item) => item.id === actualizada.id ? actualizada : item);
        this.subiendoImagenId = null;
        input.value = '';
      },
      error: (err) => {
        this.errorCanchas = err?.error?.message || 'No se pudo actualizar la foto.';
        this.subiendoImagenId = null;
        input.value = '';
      },
    });
  }

  eliminarCancha(cancha: any): void {
    this.eliminandoId = cancha.id;
    this.canchaService.eliminar(cancha.id).subscribe({
      next: () => {
        this.eliminandoId = null;
        this.cargarCanchas();
      },
      error: () => {
        this.eliminandoId = null;
        this.errorCanchas = 'No se pudo eliminar la cancha. Intentá nuevamente.';
      },
    });
  }

  confirmarReserva(reserva: any): void {
    if(!window.confirm('Confirmá solo si recibiste la transferencia. ¿Confirmar esta reserva?'))return;
    if(this.procesandoReservaId)return;
    this.procesandoReservaId = reserva.id;
    this.reservaService.confirmar(reserva.id).subscribe({
      next: () => {
        this.procesandoReservaId = null;
        this.cargarReservas();
      },
      error: (err) => {
        this.procesandoReservaId = null;
        this.errorReservas = err?.error?.message || 'No se pudo confirmar la reserva.';
      },
    });
  }

  rechazarReserva(reserva: any): void {
    if(!window.confirm('¿Querés rechazar esta reserva? Se liberará el horario.'))return;
    if(this.procesandoReservaId)return;
    this.procesandoReservaId = reserva.id;
    this.reservaService.cancelar(reserva.id).subscribe({
      next: () => {
        this.procesandoReservaId = null;
        this.cargarReservas();
      },
      error: (err) => {
        this.procesandoReservaId = null;
        this.errorReservas = err?.error?.message || 'No se pudo cancelar la reserva.';
      },
    });
  }

  get canchasActivas():number{return this.canchas.filter(c=>c.activa).length;}

  get reservasPendientes(): number {
    return this.reservas.filter((reserva) => reserva.estado === 'PENDIENTE').length;
  }

  alternarFormulario(): void {
    this.editandoId=null;
    this.reiniciarFormulario();
    this.mostrarFormulario = !this.mostrarFormulario;
    this.errorFormulario = '';
  }

  etiquetaEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      CANCELADA: 'Cancelada',
    };
    return etiquetas[estado] || estado;
  }

  etiquetaEstadoPago(estado: string | null): string {
    const etiquetas: Record<string, string> = {
      PENDIENTE: 'Pago pendiente',
      APROBADO: 'Pago aprobado',
      RECHAZADO: 'Pago rechazado',
      REEMBOLSADO: 'Reembolsado',
    };
    return estado ? etiquetas[estado] || estado : 'Sin pago iniciado';
  }

  private reiniciarFormulario(): void {
    this.nuevaCancha = {
      nombre: '', ciudadId: '', direccion: '', tipo: 'futbol 5',
      superficie: 'sintetico', cantJugadores: 10, precioHora: 0,
      techada: false, descripcion: '',
    };
    this.imagenSeleccionada = null;
    this.imagenPreview = '';
  }
}
