import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription, switchMap } from 'rxjs';
import { CanchaService } from '../../services/cancha.service';
import { ReservaService } from '../../services/reserva.service';
import { Disponibilidad, Franja, Horario, DIAS, fechaArgentina } from '../../models/agenda';
@Component({selector:'app-detalle-cancha',imports:[CommonModule,FormsModule,RouterLink],templateUrl:'./detalle-cancha.component.html',styleUrls:['./detalle-cancha.component.css','./detalle-complejo-06b.css','./detalle-disponibilidad-06d.css','./detalle-reserva-06e.css'],changeDetection:ChangeDetectionStrategy.Eager})
export class DetalleCanchaComponent implements OnInit,OnDestroy {
 cancha:any=null;otras:any[]=[];cargandoOtras=false;errorOtras='';horarios:Horario[]=[];dias=DIAS;fecha='';horaInicio='';horaFin='';disponibilidad:Disponibilidad|null=null;
 mensaje='';cargando=true;errorCarga='';consultando=false;reservando=false;confirmando=false;fechaMinima=fechaArgentina();fechaMaxima=this.calcularFechaMaxima();
 private sub?:Subscription;private consulta?:Subscription;private duenoSub?:Subscription;private horariosSub?:Subscription;
 constructor(private route:ActivatedRoute,private canchaService:CanchaService,private reservaService:ReservaService,private router:Router){}
 ngOnInit():void {
  this.sub=this.route.paramMap.pipe(switchMap(p=>{this.consulta?.unsubscribe();this.duenoSub?.unsubscribe();this.horariosSub?.unsubscribe();this.cargando=true;this.consultando=false;this.errorCarga='';this.fecha='';this.horaInicio='';this.horaFin='';this.mensaje='';this.confirmando=false;this.reservando=false;this.disponibilidad=null;this.cancha=null;this.otras=[];this.cargandoOtras=false;this.errorOtras='';this.horarios=[];return this.canchaService.obtener(Number(p.get('id')));})).subscribe({next:c=>{this.cancha=c;this.cargando=false;this.cargandoOtras=true;this.duenoSub=this.canchaService.porDueno(c.duenoId).subscribe({next:r=>{this.otras=r.filter(x=>x.id!==c.id);this.cargandoOtras=false;},error:()=>{this.cargandoOtras=false;this.errorOtras='No pudimos cargar las otras canchas del complejo.';}});this.horariosSub=this.canchaService.horarios(c.id).subscribe({next:r=>this.horarios=r,error:()=>this.mensaje='No se pudo cargar la agenda semanal.'});},error:()=>{this.cargando=false;this.errorCarga='Esta cancha no existe o está pausada.';}});
 }
 ngOnDestroy():void{this.sub?.unsubscribe();this.consulta?.unsubscribe();this.duenoSub?.unsubscribe();this.horariosSub?.unsubscribe();}
 actualizarHorarios():void{if(this.fecha&&!this.consultando&&!this.reservando)this.consultarDisponibilidad();}
 get cantidadTurnosLibres():number{return this.turnos.length;}
 consultarDisponibilidad():void {
  this.consulta?.unsubscribe();this.confirmando=false;this.disponibilidad=null;this.horaInicio='';this.horaFin='';this.mensaje='';if(!this.fecha)return;
  this.consultando=true;this.consulta=this.reservaService.disponibilidad(this.cancha.id,this.fecha).subscribe({next:d=>{this.disponibilidad=d;this.consultando=false;},error:e=>{this.consultando=false;this.mensaje=e.error?.message||'No se pudo consultar la disponibilidad. Reintentá.';}});
 }
 get turnos():Franja[]{
  const r:Franja[]=[];for(const f of this.disponibilidad?.libres||[]){let ini=Math.ceil(this.minutos(f.horaInicio)/30)*30;const fin=this.minutos(f.horaFin);for(;ini+60<=fin;ini+=30)r.push({horaInicio:this.hora(ini),horaFin:this.hora(ini+60)});}return r;
 }
 elegir(t:Franja):void {this.horaInicio=t.horaInicio;this.horaFin=t.horaFin;this.confirmando=false;this.mensaje='';}
 get seleccionValida():boolean{if(!this.horaInicio||!this.horaFin||!this.disponibilidad||this.consultando)return false;const minutos=this.minutos(this.horaFin)-this.minutos(this.horaInicio);return minutos>=30&&minutos<=240&&minutos%30===0&&this.minutos(this.horaInicio)%30===0&&(this.disponibilidad.libres.some(f=>this.horaInicio>=f.horaInicio.slice(0,5)&&this.horaFin<=f.horaFin.slice(0,5)));}
 get total():number{return this.seleccionValida?Math.round(this.cancha.precioHora*(this.minutos(this.horaFin)-this.minutos(this.horaInicio))/60*100)/100:0;}
 abrirConfirmacion():void {
  if(!this.seleccionValida||this.reservando||this.consultando)return;
  this.mensaje='';this.confirmando=true;
 }
 cancelarConfirmacion():void {if(!this.reservando)this.confirmando=false;}
 reservar():void {
  if(!this.confirmando||!this.seleccionValida||this.reservando||this.consultando)return;
  const pedido={canchaId:this.cancha.id as number,fecha:this.fecha,horaInicio:this.horaInicio,horaFin:this.horaFin};
  this.mensaje='';this.reservando=true;
  this.reservaService.disponibilidad(pedido.canchaId,pedido.fecha).subscribe({
   next:d=>{
    // La disponibilidad consultada puede haber cambiado desde que el usuario eligió el turno.
    const sigueLibre=(d.libres||[]).some(f=>this.minutos(pedido.horaInicio)>=this.minutos(f.horaInicio)&&this.minutos(pedido.horaFin)<=this.minutos(f.horaFin));
    if(!sigueLibre){
     this.disponibilidad=d;this.confirmando=false;this.reservando=false;this.horaInicio='';this.horaFin='';
     this.mensaje='El horario ya no está disponible. Elegí otro turno.';return;
    }
    this.reservaService.crear(pedido).subscribe({
     next:()=>void this.router.navigate(['/reservas']),
     error:e=>{
      this.reservando=false;this.confirmando=false;
      this.consultarDisponibilidad();
      this.mensaje=e.error?.message||'No se pudo crear la reserva. Revisá tus reservas antes de reintentar.';
     }
    });
   },
   error:()=>{this.reservando=false;this.confirmando=false;this.mensaje='No pudimos verificar el horario. Reintentá antes de reservar.';}
  });
 }
 horariosDia(d:number):Horario[]{return this.horarios.filter(h=>h.diaSemana===d);}
 ocultarImagen(e:Event):void{(e.target as HTMLImageElement).style.display='none';}
 private calcularFechaMaxima():string{const [y,m,d]=this.fechaMinima.split('-').map(Number);const fecha=new Date(Date.UTC(y,m-1,d+90));return fecha.toISOString().slice(0,10);}
 private minutos(h:string):number {const p=h.split(':').map(Number);return p[0]*60+p[1]+(p[2]||0)/60;}
 private hora(m:number):string {return `${String(Math.floor(m/60)).padStart(2,'0')}:${String(m%60).padStart(2,'0')}`;}
}
