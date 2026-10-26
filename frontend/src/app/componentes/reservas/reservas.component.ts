import { Component, OnDestroy, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { fechaArgentina } from '../../models/agenda';
import { Subscription, switchMap, take, timer } from 'rxjs';
import { DatosTransferencia, ReservaService } from '../../services/reserva.service';
import { PagoService } from '../../services/pago.service';

@Component({
    selector: 'app-reservas',
    imports: [CommonModule, RouterLink,FormsModule],
    templateUrl: './reservas.component.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    styleUrl: './reservas.component.css'
})
export class ReservasComponent implements OnInit, OnDestroy {
  filtro = 'proximas';
  busqueda = '';
  desde = '';
  hasta = '';
  orden: 'recientes' | 'antiguas' | 'importeMayor' | 'importeMenor' = 'recientes';

  get visibles(): any[] {
    const termino = this.busqueda.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    const hoy = fechaArgentina();
    return this.reservas.filter(r => {
      const fecha = String(r.fecha || '').slice(0, 10);
      const estadoValido = this.filtro === 'todas' ||
        (this.filtro === 'proximas' ? fecha >= hoy && r.estado !== 'CANCELADA' : r.estado === this.filtro);
      if (!estadoValido || (this.desde && fecha < this.desde) || (this.hasta && fecha > this.hasta)) return false;
      const texto = `${r.canchaNombre || ''} ${r.id ?? ''}`.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
      return !termino || texto.includes(termino);
    }).sort((a, b) => {
      if (this.orden === 'importeMayor') return Number(b.monto || 0) - Number(a.monto || 0);
      if (this.orden === 'importeMenor') return Number(a.monto || 0) - Number(b.monto || 0);
      const comparacion = `${a.fecha}${a.horaInicio}`.localeCompare(`${b.fecha}${b.horaInicio}`);
      return this.orden === 'antiguas' || (this.orden === 'recientes' && this.filtro === 'proximas') ? comparacion : -comparacion;
    });
  }

  limpiarFiltros(): void {
    this.filtro = 'proximas';
    this.busqueda = '';
    this.desde = '';
    this.hasta = '';
    this.orden = 'recientes';
  }

  get filtrosAdicionalesActivos(): boolean {
    return !!(this.busqueda.trim() || this.desde || this.hasta || this.orden !== 'recientes');
  }
  vencida(r:any):boolean{return !!r.expiresAt&&Date.parse(r.expiresAt)<=Date.now()&&r.estado==='PENDIENTE';}
  copiado='';
  copiar(valor:string):void{navigator.clipboard.writeText(valor).then(()=>this.copiado='Dato copiado.').catch(()=>this.copiado='Seleccioná el dato para copiarlo manualmente.');}
  reservas: any[] = [];
  cargando = true;
  error = '';
  cancelandoId: number | null = null;
  pagandoId: number | null = null;
  cargandoTransferenciaId: number | null = null;
  datosTransferencia: Record<number, DatosTransferencia> = {};
  mensajeAccion = '';
  mensajePago = '';
  tipoMensajePago: 'exito' | 'pendiente' | 'error' = 'pendiente';
  private actualizacionPago?: Subscription;

  constructor(
    private reservaService: ReservaService,
    private pagoService: PagoService,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.procesarRetornoPago();
    this.cargar();
  }

  ngOnDestroy(): void {
    this.actualizacionPago?.unsubscribe();
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.reservaService.misReservas().subscribe({
      next: (res: any) => {
        this.reservas = res.data ?? res;
        this.cargando = false;
      },
      error: () => {
        this.error = 'Revisá tu conexión e intentá nuevamente.';
        this.cargando = false;
      },
    });
  }

  cancelar(reserva: any): void {
    if(this.cancelandoId||!window.confirm('¿Querés cancelar esta reserva y liberar el horario?'))return;
    this.mensajeAccion = '';
    this.cancelandoId = reserva.id;
    this.reservaService.cancelar(reserva.id).subscribe({
      next: () => {
        this.cancelandoId = null;
        this.cargar();
      },
      error: (err) => {
        this.cancelandoId = null;
        this.mensajeAccion = err?.error?.message || 'No se pudo cancelar la reserva. Intentá nuevamente.';
      },
    });
  }

  pagar(reserva: any): void {
    if(this.pagandoId)return;
    this.mensajeAccion = '';
    this.pagandoId = reserva.id;
    this.pagoService.crearPreferencia(reserva.id).subscribe({
      next: (pago) => {
        if (!pago.initPoint || !/^https:\/\/(www\.|sandbox\.)?mercadopago\.(com\.ar|com)(\/|$)/.test(pago.initPoint)) {
          this.pagandoId = null;
          this.mensajeAccion = 'Mercado Pago no devolvió un enlace de pago válido.';
          return;
        }
        window.location.assign(pago.initPoint);
      },
      error: (err) => {
        this.pagandoId = null;
        this.mensajeAccion = err?.error?.message || 'No pudimos iniciar el pago. Intentá nuevamente.';
      },
    });
  }

  verDatosTransferencia(reserva: any): void {
    if (this.datosTransferencia[reserva.id]) {
      delete this.datosTransferencia[reserva.id];
      return;
    }

    this.mensajeAccion = '';
    this.cargandoTransferenciaId = reserva.id;
    this.reservaService.datosTransferencia(reserva.id).subscribe({
      next: (datos) => {
        this.datosTransferencia[reserva.id] = datos;
        this.cargandoTransferenciaId = null;
      },
      error: (err) => {
        this.cargandoTransferenciaId = null;
        this.mensajeAccion = err?.error?.message || 'No pudimos cargar los datos para la transferencia.';
      },
    });
  }

  puedeVerTransferencia(reserva: any): boolean {
    return !reserva.pagoRequerido && reserva.estado !== 'CANCELADA'&&!this.vencida(reserva);
  }

  puedePagar(reserva: any): boolean {
    return !this.vencida(reserva) && reserva.pagoRequerido
      && reserva.estado === 'PENDIENTE'
      && reserva.estadoPago !== 'APROBADO'
      && reserva.estadoPago !== 'REEMBOLSADO';
  }

  puedeCancelar(reserva: any): boolean {
    return reserva.estado === 'PENDIENTE' && reserva.estadoPago !== 'APROBADO';
  }

  textoBotonPago(reserva: any): string {
    if (this.pagandoId === reserva.id) return 'Abriendo Mercado Pago...';
    if (reserva.estadoPago === 'RECHAZADO') return 'Reintentar pago';
    if (reserva.estadoPago === 'PENDIENTE') return 'Continuar pago';
    return 'Pagar con Mercado Pago';
  }

  etiquetaEstadoPago(estado: string | null): string {
    const etiquetas: Record<string, string> = {
      PENDIENTE: 'Pago pendiente',
      APROBADO: 'Pago aprobado',
      RECHAZADO: 'Pago rechazado',
      REEMBOLSADO: 'Pago reembolsado',
    };
    return estado ? etiquetas[estado] || estado : '';
  }

  diaReserva(fecha: string): string {
    return fecha?.split('-')[2] || '--';
  }

  mesReserva(fecha: string): string {
    const indice = Number(fecha?.split('-')[1]) - 1;
    const meses = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];
    return meses[indice] || '---';
  }

  etiquetaEstado(estado: string): string {
    const etiquetas: Record<string, string> = {
      PENDIENTE: 'Pendiente',
      CONFIRMADA: 'Confirmada',
      CANCELADA: 'Cancelada',
    };
    return etiquetas[estado] || estado;
  }

  private procesarRetornoPago(): void {
    const resultado = this.route.snapshot.queryParamMap.get('pago');
    if (!resultado) return;

    const mensajes: Record<string, { tipo: 'exito' | 'pendiente' | 'error'; texto: string }> = {
      aprobado: {
        tipo: 'exito',
        texto: 'Mercado Pago recibió el pago. Estamos verificando la confirmación de tu reserva.',
      },
      pendiente: {
        tipo: 'pendiente',
        texto: 'El pago está siendo procesado. El estado se actualizará automáticamente.',
      },
      rechazado: {
        tipo: 'error',
        texto: 'El pago no pudo completarse. Podés intentarlo nuevamente desde tu reserva.',
      },
    };

    const mensaje = mensajes[resultado];
    if (!mensaje) return;
    this.tipoMensajePago = mensaje.tipo;
    this.mensajePago = mensaje.texto;

    if (resultado !== 'rechazado') {
      this.actualizacionPago = timer(2500, 2500).pipe(
        take(5),
        switchMap(() => this.reservaService.misReservas())
      ).subscribe({
        next: (res: any) => (this.reservas = res.data ?? res),
        error: () => undefined,
      });
    }
  }
}
