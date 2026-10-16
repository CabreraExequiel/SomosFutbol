import { Component, Input, Output, EventEmitter, OnChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { CanchaService } from '../../services/cancha.service';
import { DIAS, Horario, Bloqueo, fechaArgentina } from '../../models/agenda';
@Component({selector:'app-agenda-cancha',imports:[CommonModule,FormsModule],templateUrl:'./agenda-cancha.component.html',styleUrl:'./agenda-cancha.component.css',changeDetection:ChangeDetectionStrategy.Eager})
export class AgendaCanchaComponent implements OnChanges {
 @Input({required:true}) cancha!:any;
 @Output() cerrar=new EventEmitter<void>();
 dias=DIAS; horarios:Horario[]=[]; bloqueos:Bloqueo[]=[]; seleccion:number[]=[1,2,3,4,5];
 inicio='18:00'; fin='23:00'; fecha=''; bloqueoInicio=''; bloqueoFin=''; motivo=''; completo=true;
 cargando=true; guardando=false; bloqueando=false; error=''; mensaje=''; sucio=false; hoy=fechaArgentina();
 constructor(private service:CanchaService) {}
 ngOnChanges():void {this.cargar();}
 cargar():void {
  this.cargando=true;this.error='';this.mensaje='';this.sucio=false;
  forkJoin({horarios:this.service.horarios(this.cancha.id),bloqueos:this.service.bloqueos(this.cancha.id)}).subscribe({
   next:r=>{this.horarios=r.horarios;this.bloqueos=r.bloqueos;this.cargando=false;},
   error:()=>{this.error='No pudimos cargar la agenda. Reintentá.';this.cargando=false;}
  });
 }
 toggle(d:number):void {this.seleccion=this.seleccion.includes(d)?this.seleccion.filter(x=>x!==d):[...this.seleccion,d];}
 agregar():void {
  this.error='';this.mensaje='';
  if(!this.seleccion.length || !this.inicio || !this.fin || this.fin<=this.inicio){this.error='Elegí los días y un horario de fin posterior al inicio.';return;}
  if(this.seleccion.some(d=>this.horarios.some(h=>h.diaSemana===d && this.inicio<h.horaFin.slice(0,5) && this.fin>h.horaInicio.slice(0,5)))) {this.error='Ese rango se superpone con un horario. Quitá el anterior o elegí otro rango.';return;}
  this.horarios=[...this.horarios,...this.seleccion.map(d=>({diaSemana:d,horaInicio:this.inicio,horaFin:this.fin}))];this.sucio=true;
 }
 porDia(d:number):Horario[]{return this.horarios.filter(h=>h.diaSemana===d).sort((a,b)=>a.horaInicio.localeCompare(b.horaInicio));}
 quitar(h:Horario):void {this.horarios=this.horarios.filter(x=>x!==h);this.sucio=true;this.mensaje='';}
 guardar():void {
  if(this.guardando)return;this.guardando=true;this.error='';this.mensaje='';
  this.service.guardarHorarios(this.cancha.id,this.horarios).subscribe({next:r=>{this.horarios=r;this.guardando=false;this.sucio=false;this.mensaje='Agenda publicada. Los jugadores ya pueden reservar estos horarios.';},error:e=>{this.guardando=false;this.error=e.error?.message||'No se pudo guardar la agenda.';}});
 }
 bloquear():void {
  if(this.bloqueando)return;this.error='';this.mensaje='';
  if(!this.fecha || (!this.completo && (!this.bloqueoInicio || !this.bloqueoFin || this.bloqueoFin<=this.bloqueoInicio))){this.error='Revisá la fecha y el rango del bloqueo.';return;}
  this.bloqueando=true;
  this.service.bloquear(this.cancha.id,{fecha:this.fecha,horaInicio:this.completo?null:this.bloqueoInicio,horaFin:this.completo?null:this.bloqueoFin,motivo:this.motivo.trim()}).subscribe({next:b=>{this.bloqueos=[...this.bloqueos,b].sort((a,b)=>a.fecha.localeCompare(b.fecha));this.bloqueando=false;this.mensaje='Bloqueo agregado. Revisá las reservas existentes de esa fecha para coordinarlas.';this.fecha='';this.motivo='';},error:e=>{this.bloqueando=false;this.error=e.error?.message||'No se pudo bloquear la fecha.';}});
 }
 desbloquear(b:Bloqueo):void {
  this.service.desbloquear(this.cancha.id,b.id).subscribe({next:()=>{this.bloqueos=this.bloqueos.filter(x=>x.id!==b.id);this.mensaje='Bloqueo eliminado.';},error:e=>this.error=e.error?.message||'No se pudo quitar el bloqueo.'});
 }
 salir():void {if(!this.sucio || window.confirm('Tenés horarios sin guardar. ¿Querés cerrar la agenda?'))this.cerrar.emit();}
}
